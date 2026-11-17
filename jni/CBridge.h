#ifndef _Included_it_unisa_ocelot_simulator_CBridge
#define _Included_it_unisa_ocelot_simulator_CBridge

/* Header file for native code in class it.unisa.ocelot.simulator.CBridge */

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     it/unisa/ocelot/simulator/CBridge
 * Method:    privInit
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_it_unisa_ocelot_simulator_CBridge_privInit(JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     it/unisa/ocelot/simulator/CBridge
 * Method:    getEvents
 * Signature: (Lit/unisa/ocelot/simulator/EventsHandler;[D[[D[D)V
 */
JNIEXPORT void JNICALL Java_it_unisa_ocelot_simulator_CBridge_getEvents(JNIEnv *, jobject, jobject, jdoubleArray, jobjectArray, jdoubleArray);

/*
 * Class:     it/unisa/ocelot/simulator/CBridge
 * Method:    memoryDump
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_it_unisa_ocelot_simulator_CBridge_memoryDump(JNIEnv *, jobject);

/*
 * Class:     it/unisa/ocelot/simulator/CBridge
 * Method:    shutdown
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_it_unisa_ocelot_simulator_CBridge_shutdown(JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
