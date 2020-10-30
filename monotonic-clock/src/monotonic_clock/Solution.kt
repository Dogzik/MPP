package monotonic_clock

class Solution : MonotonicClock {
    data class Vector(var d1: Int, var d2: Int, var d3: Int) {
        override fun equals(other: Any?): Boolean {
            return if (other is Vector) {
                compareValuesBy(this, other, Vector::d1, Vector::d2, Vector:: d3) == 0
            } else {
                false
            }
        }
    }

    private var c11 by RegularInt(0)
    private var c12 by RegularInt(0)
    private var c13 by RegularInt(0)

    private var c21 by RegularInt(0)
    private var c22 by RegularInt(0)
    private var c23 by RegularInt(0)


    override fun write(time: Time) {
        c21 = time.d1
        c22 = time.d2
        c23 = time.d3

        c13 = c23
        c12 = c22
        c11 = c21
    }

    override fun read(): Time {
        val r1 = Vector(0, 0,0)
        val r2 = Vector(0, 0,0)

        r1.d1 = c11
        r1.d2 = c12
        r1.d3 = c13

        r2.d3 = c23
        r2.d2 = c22
        r2.d1 = c21

        if (r1 == r2) {
            return Time(r1.d1, r1.d2, r1.d3)
        }

        if (r1.d1 == r2.d1) {
            if (r1.d2 == r2.d2) {
                return Time(r2.d1, r2.d2, r2.d3)
            } else {
                return Time(r2.d1, r2.d2, 0)
            }
        } else {
            return Time(r2.d1, 0, 0)
        }
    }


}
