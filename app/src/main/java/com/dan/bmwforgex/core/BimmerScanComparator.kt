package com.dan.bmwforgex.core

data class LocatedCode(val module: String, val code: String)
data class ModuleMovedCode(val code: String, val from: String, val to: String)
data class ScanDelta(
    val retained: List<LocatedCode>,
    val disappeared: List<LocatedCode>,
    val appeared: List<LocatedCode>,
    val attributionChanged: List<ModuleMovedCode>,
    val earlierCount: Int,
    val laterCount: Int
)

/** Compares two snapshots by (module,code) and distinguishes module attribution changes. */
object BimmerScanComparator {
    fun compare(earlier: BimmerReport, later: BimmerReport): ScanDelta {
        val before = earlier.modules.flatMap { m -> m.faults.map { LocatedCode(m.ecu, it.code) } }.toSet()
        val after = later.modules.flatMap { m -> m.faults.map { LocatedCode(m.ecu, it.code) } }.toSet()
        val shared = before.intersect(after)
        val missing = before.subtract(shared).toMutableSet()
        val added = after.subtract(shared).toMutableSet()
        val moved = mutableListOf<ModuleMovedCode>()
        for (old in missing.toList().sortedWith(compareBy({ it.code }, { it.module }))) {
            val found = added.firstOrNull { it.code == old.code && it.module != old.module } ?: continue
            moved += ModuleMovedCode(old.code, old.module, found.module)
            missing.remove(old)
            added.remove(found)
        }
        return ScanDelta(shared.sortedWith(compareBy({ it.module }, { it.code })),
            missing.sortedWith(compareBy({ it.module }, { it.code })),
            added.sortedWith(compareBy({ it.module }, { it.code })), moved,
            earlier.faultCount, later.faultCount)
    }
}
