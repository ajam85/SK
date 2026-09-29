package com.example.straidlov_alchimie_kalkulaka.model

enum class CombinationStatus(val labelCz: String) {
    UNTESTED("Netestováno"),
    // Keep these ordinals aligned with the original FAILED/SUCCESS values so
    // existing local JSON backups remain readable after the terminology change.
    NONFUNCTIONAL("Nefunkční recept"),
    FUNCTIONAL("Funkční recept");

    companion object {
        fun fromInt(value: Int): CombinationStatus {
            return entries.getOrElse(value) { UNTESTED }
        }
    }
}
