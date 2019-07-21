#include <stdlib.h>
#include <math.h>
#include <fenv.h>

#if __ANDROID_API__ < 18
double log2(double x) {
    return log(x) * 1.442695040888963407359924681001892137L;
}
#endif /* __ANDROID_API__ < 18 */

#define MAX_EE 500

static inline int ee(double x) {
    return ((int) (log10(fabs(x)) + MAX_EE)) - MAX_EE;
}

#undef MAX_EE

static unsigned int base_two_digits(unsigned int x) {
    return x ? 32 - __builtin_clz(x) : 0;
}

static int eel(int x) {
    static const unsigned char guess[33] = {
            0, 0, 0, 0, 1, 1, 1, 2, 2, 2,
            3, 3, 3, 3, 4, 4, 4, 5, 5, 5,
            6, 6, 6, 6, 7, 7, 7, 8, 8, 8,
            9, 9, 9
    };
    static const unsigned int ten_to_the[] = {
            1, 10, 100, 1000, 10000, 100000,
            1000000, 10000000, 100000000, 1000000000,
    };
    unsigned int ux = (unsigned int) abs(x);
    unsigned int digits = guess[base_two_digits(ux)];
    return digits + (ux >= ten_to_the[digits]) - 1;
}

// d * pow(10, n)
static inline double multiply_pow10(double d, int n) {
    if (n > 0) {
        long long multiple = 1;
        while (--n >= 0) {
            multiple *= 10;
        }
        d *= multiple;
    } else if (n < 0) {
        long long multiple = 1;
        while (++n <= 0) {
            multiple *= 10;
        }
        d /= multiple;
    }
    return d;
}

static void js_e_string_zero(int sign, int n_digits, char *buf, int buf_size) {
    // Check length
    if (buf_size <= n_digits + 5 + (n_digits > 1)) {
        if (buf_size > 0) {
            buf[0] = '\0';
        }
        return;
    }

    *buf++ = (char) (sign == 0 ? '+' : '-');
    *buf++ = '0';
    if (n_digits > 1) {
        *buf++ = (char) '.';
        while (--n_digits > 0) {
            *buf++ = '0';
        }
    }
    *buf++ = 'e';
    *buf++ = '+';
    *buf++ = '0';
    *buf++ = '0';
    *buf = '\0';
}

static void js_e_string_by_hand(double d, int n_digits, char *buf, int buf_size) {
    if (n_digits <= 0) {
        n_digits = 7;
    }

    if (d == 0) {
        js_e_string_zero(signbit(d), n_digits, buf, buf_size);
        return;
    }

    int old_ee = ee(d);
    int new_int = (int) rint(multiply_pow10(d, n_digits - old_ee - 1));
    int new_ee = eel(new_int) + 1 - n_digits + old_ee;

    if (new_ee > old_ee) {
        new_int /= 10;
    }

    char new_ee_sign = (char) (new_ee >= 0 ? '+' : '-');
    int new_ee_number = abs(new_ee);

    if (n_digits == 1) {
        snprintf(buf, buf_size, "%+de%c%02d", new_int, new_ee_sign, new_ee_number);
    } else {
        snprintf(buf, buf_size, ".%+de%c%02d", new_int, new_ee_sign, new_ee_number);
        buf[0] = buf[1];
        buf[1] = buf[2];
        buf[2] = '.';
    }
}

static void js_e_string_tonearest(double d, int n_digits, char *buf, int buf_size) {
    snprintf(buf, buf_size, "%+.*e", n_digits - 1, d);
}

void js_e_string(double d, int n_digits, int rounding_mode, char *buf, int buf_size) {
    if (rounding_mode == FE_TONEAREST) {
        js_e_string_tonearest(d, n_digits, buf, buf_size);
    } else {
        js_e_string_by_hand(d, n_digits, buf, buf_size);
    }
}
