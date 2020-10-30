package mutex

import java.util.*

/**
 * Stub for distributed mutual exclusion implementation.
 * All functions are called from the single main thread.
 */
class ProcessImpl(private val env: Environment) : Process {
    enum class ForkSate { CLEAN, DIRTY, TAKEN }
    enum class MsgType { REQ, OK }

    private val forks = Array(env.nProcesses + 1) { if (env.processId < it) ForkSate.DIRTY else ForkSate.TAKEN }
    private val forkReqs = BooleanArray(env.nProcesses + 1) { false }
    private var inCS = false
    private var wantCS = false
    private var needForks = 0

    override fun onMessage(srcId: Int, message: Message) {
        val type = message.parse { readEnum<MsgType>() }
        when (type) {
            MsgType.OK -> processOk(srcId)
            MsgType.REQ -> processReq(srcId)
        }
    }

    private fun processReq(srcId: Int) {
        when (forks[srcId]) {
            ForkSate.TAKEN -> throw IllegalStateException("Process $srcId asked for taken fork")
            ForkSate.CLEAN -> {
                check(!forkReqs[srcId]) { "Process $srcId already asked for this" }
                if (wantCS || inCS) {
                    forkReqs[srcId] = true
                } else {
                    forks[srcId] = ForkSate.TAKEN
                    sendMsg(srcId, MsgType.OK)
                }
            }
            ForkSate.DIRTY -> {
                check(!forkReqs[srcId]) { "Process $srcId already asked for this" }
                if (inCS) {
                    forkReqs[srcId] = true
                } else {
                    forks[srcId] = ForkSate.TAKEN
                    sendMsg(srcId, MsgType.OK)
                    if (wantCS) {
                        ++needForks
                        sendMsg(srcId, MsgType.REQ)
                    }
                }
            }
        }
    }

    private fun processOk(srcId: Int) {
        check(wantCS) { "Thanks process $srcId, but I never asked for this" }
        forks[srcId] = ForkSate.CLEAN
        --needForks
        if (needForks == 0) {
            wantCS = false
            inCS = true
            env.locked()
        }
    }

    override fun onLockRequest() {
        check(!(inCS || wantCS)) { "Lock was already requested" }
        wantCS = true
        allEdges().forEach {
            if (forks[it] == ForkSate.TAKEN) {
                sendMsg(it, MsgType.REQ)
                ++needForks
            }
        }
        if (needForks == 0) {
            env.locked()
            wantCS = false
            inCS = true
        }
    }

    override fun onUnlockRequest() {
        check(inCS) { "We are not in critical section" }
        env.unlocked()
        inCS = false
        allEdges().forEach {
            if (forkReqs[it]) {
                forkReqs[it] = false
                forks[it] = ForkSate.TAKEN
                sendMsg(it, MsgType.OK)
            } else {
                forks[it] = ForkSate.DIRTY
            }
        }
    }

    private fun sendMsg(dst: Int, type: MsgType) {
        env.send(dst) {
            writeEnum(type)
        }
    }

    private fun allEdges() = (1..env.nProcesses).filter { it != env.processId }
}
