#if __ANDROID_API__ < 18
#include <math.h>
double log2(double x) {
    return log(x) * 1.442695040888963407359924681001892137L;
}
#endif /* __ANDROID_API__ < 18 */
