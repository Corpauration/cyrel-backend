package fr.corpauration.version

class CyrelVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val canal: CyrelCanal = CyrelCanal.STABLE,
    val canalVersion: Int = 1
) {
    companion object {
        fun fromString(version: String): CyrelVersion {
            val versionParts = version.split("-")
            val baseVersion = versionParts.first().split(".")
            val canal = if (versionParts.size == 1) CyrelCanal.STABLE else {
                when (versionParts.last().split(".").first()) {
                    "alpha" -> CyrelCanal.ALPHA
                    "beta" -> CyrelCanal.BETA
                    else -> throw UnknownCanal()
                }
            }
            return CyrelVersion(
                baseVersion[0].toInt(),
                baseVersion[1].toInt(),
                baseVersion[2].toInt(),
                canal,
                if (canal == CyrelCanal.STABLE) 1 else versionParts.last().split(".").last().toInt()
            )
        }

        public const val MAX_COMPONENT_VALUE = 255
    }

    override fun toString(): String {
        val baseVersion = "$major.$minor.$patch"
        return when (canal) {
            CyrelCanal.STABLE -> baseVersion
            CyrelCanal.ALPHA -> "$baseVersion-alpha.$canalVersion"
            CyrelCanal.BETA -> "$baseVersion-beta.$canalVersion"
        }
    }

    fun toInt(): Int {
        require(major in 0..MAX_COMPONENT_VALUE && minor in 0..MAX_COMPONENT_VALUE && patch in 0..MAX_COMPONENT_VALUE) {
            "Version components are out of range: $major.$minor.$patch"
        }
        return major.shl(16) + minor.shl(8) + patch
    }

    operator fun compareTo(b: CyrelVersion): Int {
        if (toInt() == b.toInt()) {
            when (canal) {
                CyrelCanal.STABLE -> {
                    return when (b.canal) {
                        CyrelCanal.STABLE -> 0
                        else -> -1
                    }
                }

                CyrelCanal.BETA -> {
                    return when (b.canal) {
                        CyrelCanal.STABLE -> -1
                        CyrelCanal.BETA -> canalVersion - b.canalVersion
                        CyrelCanal.ALPHA -> 0
                    }
                }

                CyrelCanal.ALPHA -> {
                    return when (b.canal) {
                        CyrelCanal.STABLE -> -1
                        CyrelCanal.BETA -> 0
                        CyrelCanal.ALPHA -> canalVersion - b.canalVersion
                    }
                }
            }
        } else {
            return toInt() - b.toInt()
        }
    }

    fun isAtLeast(major: Int, minor: Int): Boolean =
        this.major > major || (this.major == major &&
                this.minor >= minor)
}