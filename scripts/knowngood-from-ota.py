#!/usr/bin/env python3
"""Builds known-good lines from a vendor's full OTA zip without downloading all of it.

    scripts/knowngood-from-ota.py URL WORKDIR [partition ...] > lines.sha256

Reads payload.bin inside the zip over HTTP range requests, writes only the named partitions (default: system,
system_ext, product, vendor, odm, mi_ext), checks every operation's SHA-256 and each partition's hash from the
signed manifest, unpacks the EROFS images with fsck.erofs and prints "sha256  /partition/path" for every APK, JAR,
ODEX, VDEX and .so file, in the format of app/src/main/assets/iocs/knowngood-system.sha256.

Needs fsck.erofs (erofs-utils; FSCK_EROFS overrides the path) and zstd/xz/bzip2 only through Python's stdlib or
the zstd binary. Only full OTAs: diff operations are refused.
"""
import bz2
import hashlib
import lzma
import os
import shutil
import struct
import subprocess
import sys
import urllib.request
import zipfile
from pathlib import Path

DEFAULT_PARTITIONS = ["system", "system_ext", "product", "vendor", "odm", "mi_ext"]
KEEP = (".apk", ".jar", ".odex", ".vdex", ".so")
REPLACE, REPLACE_BZ, ZERO, DISCARD, REPLACE_XZ, REPLACE_ZSTD = 0, 1, 6, 7, 8, 14


class HttpFile:
    """Seekable read-only view of a remote file through Range requests."""

    def __init__(self, url):
        self.url, self.pos = url, 0
        req = urllib.request.Request(url, method="HEAD")
        with urllib.request.urlopen(req, timeout=60) as r:
            self.size = int(r.headers["Content-Length"])

    def seek(self, off, whence=0):
        self.pos = {0: off, 1: self.pos + off, 2: self.size + off}[whence]
        return self.pos

    def tell(self):
        return self.pos

    def read(self, n=-1):
        if n < 0:
            n = self.size - self.pos
        if n == 0 or self.pos >= self.size:
            return b""
        data = b"".join(self.stream(self.pos, n))
        self.pos += len(data)
        return data

    def stream(self, start, length, chunk=1 << 20):
        req = urllib.request.Request(self.url, headers={"Range": f"bytes={start}-{start + length - 1}"})
        with urllib.request.urlopen(req, timeout=120) as r:
            if r.status != 206:
                raise IOError(f"server ignored the range request (HTTP {r.status})")
            while True:
                b = r.read(chunk)
                if not b:
                    break
                yield b


def varint(buf, i):
    shift = value = 0
    while True:
        b = buf[i]
        i += 1
        value |= (b & 0x7F) << shift
        if b < 0x80:
            return value, i
        shift += 7


def fields(buf):
    """Minimal protobuf reader: yields (field number, value) with value int or bytes."""
    i = 0
    while i < len(buf):
        key, i = varint(buf, i)
        num, wire = key >> 3, key & 7
        if wire == 0:
            v, i = varint(buf, i)
        elif wire == 1:
            v, i = struct.unpack_from("<Q", buf, i)[0], i + 8
        elif wire == 2:
            n, i = varint(buf, i)
            v, i = buf[i:i + n], i + n
        elif wire == 5:
            v, i = struct.unpack_from("<I", buf, i)[0], i + 4
        else:
            raise ValueError(f"unsupported wire type {wire}")
        yield num, v


def extent(buf):
    f = dict(fields(buf))
    return f.get(1, 0), f.get(2, 0)


def operation(buf):
    op = {"type": 0, "offset": 0, "length": 0, "dst": [], "hash": None}
    for num, v in fields(buf):
        if num == 1: op["type"] = v
        elif num == 2: op["offset"] = v
        elif num == 3: op["length"] = v
        elif num == 6: op["dst"].append(extent(v))
        elif num == 8: op["hash"] = v
    return op


def partition(buf):
    p = {"name": "", "ops": [], "size": 0, "hash": None}
    for num, v in fields(buf):
        if num == 1: p["name"] = v.decode()
        elif num == 7:
            info = dict(fields(v))
            p["size"], p["hash"] = info.get(1, 0), info.get(2)
        elif num == 8: p["ops"].append(operation(v))
    return p


def payload_offset(f):
    z = zipfile.ZipFile(f)
    info = z.getinfo("payload.bin")
    if info.compress_type != zipfile.ZIP_STORED:
        raise ValueError("payload.bin is compressed inside the zip")
    f.seek(info.header_offset)
    local = f.read(30)
    name_len, extra_len = struct.unpack_from("<HH", local, 26)
    return info.header_offset + 30 + name_len + extra_len


def decompress(kind, data):
    if kind == REPLACE: return data
    if kind == REPLACE_XZ: return lzma.decompress(data)
    if kind == REPLACE_BZ: return bz2.decompress(data)
    if kind == REPLACE_ZSTD:
        return subprocess.run(["zstd", "-dc"], input=data, capture_output=True, check=True).stdout
    raise ValueError(f"operation type {kind} is not a full-OTA operation")


def write_partition(f, data_base, block, p, out):
    ops = sorted(p["ops"], key=lambda o: o["offset"])
    with open(out, "wb") as img:
        img.truncate(p["size"])
        data_ops = [o for o in ops if o["type"] not in (ZERO, DISCARD)]
        if data_ops:
            start = data_ops[0]["offset"]
            end = max(o["offset"] + o["length"] for o in data_ops)
            stream, buf, pos = f.stream(data_base + start, end - start), bytearray(), start
            for o in data_ops:
                while len(buf) < (o["offset"] - pos) + o["length"]:
                    buf += next(stream)
                skip = o["offset"] - pos
                raw = bytes(buf[skip:skip + o["length"]])
                del buf[:skip + o["length"]]
                pos = o["offset"] + o["length"]
                if o["hash"] and hashlib.sha256(raw).digest() != o["hash"]:
                    raise IOError(f'{p["name"]}: operation data does not match its hash')
                data = decompress(o["type"], raw)
                for start_block, num_blocks in o["dst"]:
                    img.seek(start_block * block)
                    img.write(data[:num_blocks * block])
                    data = data[num_blocks * block:]
    if p["hash"]:
        h = hashlib.sha256()
        with open(out, "rb") as img:
            for chunk in iter(lambda: img.read(1 << 20), b""):
                h.update(chunk)
        if h.digest() != p["hash"]:
            raise IOError(f'{p["name"]}: image does not match the hash in the manifest')


def main():
    url, work = sys.argv[1], Path(sys.argv[2])
    wanted = sys.argv[3:] or DEFAULT_PARTITIONS
    fsck = os.environ.get("FSCK_EROFS", "fsck.erofs")
    work.mkdir(parents=True, exist_ok=True)
    f = HttpFile(url)
    base = payload_offset(f)
    f.seek(base)
    magic, version, manifest_size, sig_size = struct.unpack(">4sQQI", f.read(24))
    if magic != b"CrAU" or version != 2:
        raise ValueError("not a version 2 payload")
    manifest = f.read(manifest_size)
    data_base = base + 24 + manifest_size + sig_size
    block, parts = 4096, []
    for num, v in fields(manifest):
        if num == 3: block = v
        elif num == 13: parts.append(partition(v))
    for p in parts:
        if p["name"] not in wanted:
            continue
        img = work / f'{p["name"]}.img'
        print(f'{p["name"]}: {p["size"] >> 20} MiB', file=sys.stderr)
        write_partition(f, data_base, block, p, img)
        root = work / p["name"]
        shutil.rmtree(root, ignore_errors=True)
        subprocess.run([fsck, f"--extract={root}", str(img)], check=True, capture_output=True)
        img.unlink()
        # System-as-root images keep the real /system one level down, next to the ramdisk files.
        base = root / "system" if (root / "system" / "build.prop").is_file() else root
        for path in sorted(base.rglob("*")):
            if path.is_file() and not path.is_symlink() and path.name.endswith(KEEP):
                digest = hashlib.sha256(path.read_bytes()).hexdigest()
                print(f'{digest}  /{p["name"]}/{path.relative_to(base)}')
        shutil.rmtree(root)


if __name__ == "__main__":
    main()
