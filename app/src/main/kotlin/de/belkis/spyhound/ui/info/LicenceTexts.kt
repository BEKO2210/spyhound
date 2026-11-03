package de.belkis.spyhound.ui.info

import de.belkis.spyhound.R

/** One licence or notice file shipped in `assets/licenses`, shown word for word in "About". */
data class LicenceText(val title: Int, val asset: String)

/**
 * The original texts the app has to carry: its own licence and NOTICE (indicator sources and libraries,
 * kept identical to the files in the repository root by a unit test) and the font licences.
 */
val licenceTexts = listOf(
    LicenceText(R.string.info_licence_app, "licenses/LICENSE.txt"),
    LicenceText(R.string.info_licence_notice, "licenses/NOTICE.txt"),
    LicenceText(R.string.info_licence_inter, "licenses/OFL-Inter.txt"),
    LicenceText(R.string.info_licence_space_grotesk, "licenses/OFL-SpaceGrotesk.txt"),
)
