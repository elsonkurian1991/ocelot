/*
 * ATTENTION:
 * The Java builder (it.unisa.ocelot.runnable.Build) will enrich this meta-c file with:
 * 1) The definition of the OCELOT_CORES macro constant;
 * 2) The definition of the OCELOT_ARRAY_LENGTH macro constant;
 * 3) The definition of the EXECUTE_OCELOT_TEST macro.
 * The order is relevant.
 */
 
//uncomment for debugging/logging
#define DEBUG
//#define LOG

#include "CBridge.h"
 
//Standard C and POSIX
#include <inttypes.h>
#include <stdbool.h>
#include <stddef.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <errno.h>
#include <pthread.h>
#include <signal.h>

//Standard C (up to C23)
#include <stdalign.h>

//POSIX
#include <fcntl.h>
#include <sys/ipc.h>
#include <sys/mman.h>
#include <sys/shm.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

//TODO Windows - does it also support POSIX?

#include "lists.h"
#include "main.h"
#include "ocelot.h"
#include "pointers.h"

#define OCELOT_ARGUMENT_VALUE(i) (*(values + i))
#define OCELOT_ARGUMENT_ARRAY(i) (*(arrays + i))
#define OCELOT_ARGUMENT_POINTER(i) (*(pointers + i))
#define OCELOT_NUM(object) object

//not defined on macOS
#if !defined(SHM_FAILED)
#define SHM_FAILED ((void *)(intptr_t) -1)
#endif

//not POSIX
#define NODEBUGINFO -324354

#define NO_PID 0
#define NO_SHM_ID -1

#define TXT_BUF_SIZE 2000

#define TYPE_INT "0"
#define TYPE_SIGNED_INT "1"
#define TYPE_UNSIGNED_INT "2"
#define TYPE_SHORT "3"
#define TYPE_SIGNED_SHORT "4"
#define TYPE_UNSIGNED_SHORT "5"
#define TYPE_LONG "6"
#define TYPE_SIGNED_LONG "7"
#define TYPE_UNSIGNED_LONG "8"
#define TYPE_LONGLONG "9"
#define TYPE_SIGNED_LONGLONG "10"
#define TYPE_UNSIGNED_LONGLONG "11"
#define TYPE_CHAR "12"
#define TYPE_SIGNED_CHAR "13"
#define TYPE_UNSIGNED_CHAR "14"
#define TYPE_BOOL "15"
#define TYPE_FLOAT "16"
#define TYPE_DOUBLE "17"
#define TYPE_LONGDOUBLE "18"
#define TYPE_VOID "19"
#define TYPE_ARRAYOF_ "20"
#define TYPE_POINTERTO_ "21"
#define TYPE_STRUCT_ "22"
#define TYPE_UNION_ "23"
#define TYPE_ENUM_ "24"

#define TYPE_ARRAYOF(type) TYPE_ARRAYOF_ ":" type
#define TYPE_POINTERTO(type) TYPE_POINTERTO_ ":" type
#define TYPE_STRUCT(name) TYPE_STRUCT_ ":" name
#define TYPE_UNION(name) TYPE_UNION_ ":" name
#define TYPE_ENUM(name) TYPE_ENUM_ ":" name

#define TYPE_INT_N 0
#define TYPE_SIGNED_INT_N 1
#define TYPE_UNSIGNED_INT_N 2
#define TYPE_SHORT_N 3
#define TYPE_SIGNED_SHORT_N 4
#define TYPE_UNSIGNED_SHORT_N 5
#define TYPE_LONG_N 6
#define TYPE_SIGNED_LONG_N 7
#define TYPE_UNSIGNED_LONG_N 8
#define TYPE_LONGLONG_N 9
#define TYPE_SIGNED_LONGLONG_N 10
#define TYPE_UNSIGNED_LONGLONG_N 11
#define TYPE_CHAR_N 12
#define TYPE_SIGNED_CHAR_N 13
#define TYPE_UNSIGNED_CHAR_N 14
#define TYPE_BOOL_N 15
#define TYPE_FLOAT_N 16
#define TYPE_DOUBLE_N 17
#define TYPE_LONGDOUBLE_N 18
#define TYPE_VOID_N 19
#define TYPE_ARRAYOF_N 20
#define TYPE_POINTERTO_N 21
#define TYPE_STRUCT_N 22
#define TYPE_UNION_N 23
#define TYPE_ENUM_N 24

//TODO it is not a good idea to have logging with FILE *! 
#ifdef LOG
#define LOG_PARENT_OPEN do { \
	fd_logger_parent = open("./OCELOT_LOGGER_P.txt", O_WRONLY|O_CREAT|O_TRUNC|O_CLOFORK, S_IRUSR|S_IWUSR|S_IRGRP|S_IWGRP|S_IROTH|S_IWOTH); \
	if (fd_logger_parent == -1) { \
		_f_ocelot_debug("Unable to open/create PARENT log file\n", NODEBUGINFO); \
	} \
} while (0)
#define LOG_CHILD_OPEN(i) do { \
	char file_name[TXT_BUF_SIZE]; \
	sprintf(file_name, "./OCELOT_LOGGER_C%zu.txt", i); \
	fd_logger_child = open(file_name, O_WRONLY|O_CREAT|O_TRUNC, S_IRUSR|S_IWUSR|S_IRGRP|S_IWGRP|S_IROTH|S_IWOTH); \
	if (fd_logger_child == -1) { \
		_f_ocelot_debug("Unable to open/create CHILD %zu log file\n", i); \
	} \
} while (0)
#define LOG_MESSAGE(message, logger) do { \
	if (logger != -1) { \
		_f_ocelot_writeall(logger, message, strnlen(message, TXT_BUF_SIZE)); \
	} \
} while (0)
#define LOG_EVENT(k, logger) do { \
	if (logger != -1) { \
		char log_txt[TXT_BUF_SIZE]; \
		sprintf(log_txt, "Event #%zu:\n", k); \
		_f_ocelot_writeall(logger, log_txt, strnlen(log_txt, TXT_BUF_SIZE)); \
		sprintf(log_txt, "\tkind: %s\n", (GETEVENTKIND(k) == OCELOT_KIND_STDEV ? "std" : GETEVENTKIND(k) == OCELOT_KIND_CASEV ? "case" : "?")); \
		_f_ocelot_writeall(logger, log_txt, strnlen(log_txt, TXT_BUF_SIZE)); \
		if (GETEVENTKIND(k) == OCELOT_KIND_STDEV) { \
			sprintf(log_txt, "\tchoice: %d\n", GETEVENTSTDCHOICE(k)); \
			_f_ocelot_writeall(logger, log_txt, strnlen(log_txt, TXT_BUF_SIZE)); \
			sprintf(log_txt, "\tdistance_true: %f\n", GETEVENTSTDDISTANCETRUE(k)); \
			_f_ocelot_writeall(logger, log_txt, strnlen(log_txt, TXT_BUF_SIZE)); \
			sprintf(log_txt, "\tdistance_false: %f\n", GETEVENTSTDDISTANCEFALSE(k)); \
			_f_ocelot_writeall(logger, log_txt, strnlen(log_txt, TXT_BUF_SIZE)); \
		} else if (GETEVENTKIND(k) == OCELOT_KIND_CASEV) { \
			sprintf(log_txt, "\tchoice: %d\n", GETEVENTCASECHOICE(k)); \
			_f_ocelot_writeall(logger, log_txt, strnlen(log_txt, TXT_BUF_SIZE)); \
			sprintf(log_txt, "\tdistance: %f\n", GETEVENTCASEDISTANCE(k)); \
			_f_ocelot_writeall(logger, log_txt, strnlen(log_txt, TXT_BUF_SIZE)); \
			sprintf(log_txt, "\tchosen: %s\n", (GETEVENTCASECHOSEN(k) ? "true" : "false")); \
			_f_ocelot_writeall(logger, log_txt, strnlen(log_txt, TXT_BUF_SIZE)); \
		} else { \
			sprintf(log_txt, "\traw data:"); \
			_f_ocelot_writeall(logger, log_txt, strnlen(log_txt, TXT_BUF_SIZE)); \
			uint8_t *evt_raw = (uint8_t *) EVENT(k); \
			for (size_t n = 0; n < sizeof(_T_ocelot_event_data); ++n) { \
				if (n % 16 == 0) { \
					sprintf(log_txt, "\n\t%" PRIx8, evt_raw[n]); \
					_f_ocelot_writeall(logger, log_txt, strnlen(log_txt, TXT_BUF_SIZE)); \
				} else { \
					sprintf(log_txt, " %" PRIx8, evt_raw[n]); \
					_f_ocelot_writeall(logger, log_txt, strnlen(log_txt, TXT_BUF_SIZE)); \
				} \
			} \
			sprintf(log_txt, "\n"); \
			_f_ocelot_writeall(logger, log_txt, strnlen(log_txt, TXT_BUF_SIZE)); \
		} \
	} \
} while (0)
#define LOG_SEPARATOR(logger) do { \
	if (logger != -1) { \
		char log_txt[] = "########################\n\n"; \
		_f_ocelot_writeall(logger, log_txt, strnlen(log_txt, TXT_BUF_SIZE)); \
	} \
} while (0)
#define LOG_PARENT_CLOSE do { \
	if (fd_logger_parent != -1) { \
		close(fd_logger_parent); \
	} \
} while (0)
#define LOG_CHILD_CLOSE do { \
	if (fd_logger_child != -1) { \
		close(fd_logger_child); \
	} \
} while (0)
#else
#define LOG_PARENT_OPEN { }
#define LOG_CHILD_OPEN(i) { }
#define LOG_MESSAGE(message, logger) { }
#define LOG_EVENT(k, logger) { }
#define LOG_SEPARATOR(logger) { }
#define LOG_PARENT_CLOSE { }
#define LOG_CHILD_CLOSE { }
#endif

#define TIMEOUT 1000000
#define TIMEOUT_GRANULARITY 100000
#define OCELOT_SLEEP usleep(TIMEOUT / TIMEOUT_GRANULARITY)

#define MAX_ATTEMPTS 100

#define ERROR_CODE_OK 0
#define ERROR_CODE_SHM_KEY -1
#define ERROR_CODE_SHM_GET -2
#define ERROR_CODE_SHM_ATTACH -3
#define ERROR_CODE_SHM_CTL -4
#define ERROR_CODE_SHM_NONE -5
#define ERROR_CODE_LOCK_FAILED -6
#define ERROR_CODE_WAIT_FAILED -7
#define ERROR_CODE_NOTIFY_FAILED -8
#define ERROR_CODE_DATA_INVALID_CALL_LENGTH -9
#define ERROR_CODE_DATA_INVALID_RETURN_LENGTH -10
#define ERROR_CODE_FORK_TEMP -11
#define ERROR_CODE_FORK_NO_MEMORY -12
#define ERROR_CODE_UNEXPECTED -999

#define MEM(base, ofst) (base + ofst)

#define CALLMUTEX (v_call.mutex)
#define CALLREADY (v_call.ready)
#define CALLMSG (v_call.msg)
#define VALUESLENGTH (v_call.values_length)
#define ARRAYSLENGTH (v_call.arrays_length)
#define POINTERSLENGTH (v_call.pointers_length)
#define VALUESOFST (v_call.values_ofst)
#define ARRAYSOFST (v_call.arrays_ofst)
#define POINTERSOFST (v_call.pointers_ofst)
#define VALUES (v_call.values)
#define ARRAYS (v_call.arrays)
#define POINTERS (v_call.pointers)
#define RETURNMUTEX (v_return.mutex)
#define RETURNREADY (v_return.ready)
#define RETURNMSG (v_return.msg)
#define EVENTSLENGTH (v_return.events_length)
#define EVENTS (v_return.events)

#define VALUE(index) MEM(VALUES, index)
#define ARRAY(narray) MEM(ARRAYS, narray * OCELOT_ARRAY_LENGTH)
#define ARRAYSLOT(narray, index) MEM(ARRAY(narray), index)
#define POINTER(index) MEM(POINTERS, index)
#define EVENT(nevent) MEM(EVENTS, nevent)
#define EVENTKIND(nevent) (&(EVENT(nevent)->kind))
#define EVENTSTDCHOICE(nevent) (&(EVENT(nevent)->d.s.choice))
#define EVENTSTDDISTANCETRUE(nevent) (&(EVENT(nevent)->d.s.distance_true))
#define EVENTSTDDISTANCEFALSE(nevent) (&(EVENT(nevent)->d.s.distance_false))
#define EVENTCASECHOICE(nevent) (&(EVENT(nevent)->d.c.choice))
#define EVENTCASEDISTANCE(nevent) (&(EVENT(nevent)->d.c.distance))
#define EVENTCASECHOSEN(nevent) (&(EVENT(nevent)->d.c.chosen))

#define GET(slot) (*(slot))
#define SET(slot, value) (*(slot)) = value

#define GETCALLMSG GET(CALLMSG)
#define SETCALLMSG(value) SET(CALLMSG, value)
#define GETVALUESLENGTH GET(VALUESLENGTH)
#define SETVALUESLENGTH(value) SET(VALUESLENGTH, value)
#define GETARRAYSLENGTH GET(ARRAYSLENGTH)
#define SETARRAYSLENGTH(value) SET(ARRAYSLENGTH, value)
#define GETPOINTERSLENGTH GET(POINTERSLENGTH)
#define SETPOINTERSLENGTH(value) SET(POINTERSLENGTH, value)
#define GETVALUESOFST GET(VALUESOFST)
#define SETVALUESOFST(value) SET(VALUESOFST, value)
#define GETARRAYSOFST GET(ARRAYSOFST)
#define SETARRAYSOFST(value) SET(ARRAYSOFST, value)
#define GETPOINTERSOFST GET(POINTERSOFST)
#define SETPOINTERSOFST(value) SET(POINTERSOFST, value)
#define GETVALUE(index) GET(VALUE(index))
#define SETVALUE(index, value) SET(VALUE(index), value)
#define GETARRAY(narray, index) GET(ARRAYSLOT(narray, index))
#define SETARRAY(narray, index, value) SET(ARRAYSLOT(narray, index), value)
#define GETPOINTER(index) GET(POINTER(index))
#define SETPOINTER(index, value) SET(POINTER(index), value)

#define GETRETURNMSG GET(RETURNMSG)
#define SETRETURNMSG(value) SET(RETURNMSG, value)
#define GETEVENTSLENGTH GET(EVENTSLENGTH)
#define SETEVENTSLENGTH(value) SET(EVENTSLENGTH, value)
#define GETEVENTKIND(nevent) GET(EVENTKIND(nevent))
#define SETEVENTKIND(nevent, value) SET(EVENTKIND(nevent), value)
#define GETEVENTSTDCHOICE(nevent) GET(EVENTSTDCHOICE(nevent))
#define SETEVENTSTDCHOICE(nevent, value) SET(EVENTSTDCHOICE(nevent), value)
#define GETEVENTSTDDISTANCETRUE(nevent) GET(EVENTSTDDISTANCETRUE(nevent))
#define SETEVENTSTDDISTANCETRUE(nevent, value) SET(EVENTSTDDISTANCETRUE(nevent), value)
#define GETEVENTSTDDISTANCEFALSE(nevent) GET(EVENTSTDDISTANCEFALSE(nevent))
#define SETEVENTSTDDISTANCEFALSE(nevent, value) SET(EVENTSTDDISTANCEFALSE(nevent), value)
#define GETEVENTCASECHOICE(nevent) GET(EVENTCASECHOICE(nevent))
#define SETEVENTCASECHOICE(nevent, value) SET(EVENTCASECHOICE(nevent), value)
#define GETEVENTCASEDISTANCE(nevent) GET(EVENTCASEDISTANCE(nevent))
#define SETEVENTCASEDISTANCE(nevent, value) SET(EVENTCASEDISTANCE(nevent), value)
#define GETEVENTCASECHOSEN(nevent) GET(EVENTCASECHOSEN(nevent))
#define SETEVENTCASECHOSEN(nevent, value) SET(EVENTCASECHOSEN(nevent), value)

#define LOCK_CALL_MEM _f_ocelot_lock_shm_call(&v_call)
#define UNLOCK_CALL_MEM _f_ocelot_unlock_shm_call(&v_call)
#define WAIT_CALL_MEM_SEND _f_ocelot_wait_shm_call(&v_call, true)
#define WAIT_CALL_MEM_RECV _f_ocelot_wait_shm_call(&v_call, false)
#define NOTIFY_CALL_MEM _f_ocelot_notify_shm_call(&v_call)

#define LOCK_RETURN_MEM _f_ocelot_lock_shm_return(&v_return)
#define UNLOCK_RETURN_MEM _f_ocelot_unlock_shm_return(&v_return)
#define WAIT_RETURN_MEM_SEND _f_ocelot_wait_shm_return(&v_return, true)
#define WAIT_RETURN_MEM_RECV _f_ocelot_wait_shm_return(&v_return, false)
#define NOTIFY_RETURN_MEM _f_ocelot_notify_shm_return(&v_return)

#define GRAB_CALL_MEM_SEND do {\
	LOCK_CALL_MEM; \
	WAIT_CALL_MEM_SEND; \
} while (0)
#define GRAB_CALL_MEM_RECV do {\
	LOCK_CALL_MEM; \
	WAIT_CALL_MEM_RECV; \
} while (0)
#define RELEASE_CALL_MEM do { \
	NOTIFY_CALL_MEM; \
	UNLOCK_CALL_MEM; \
} while (0)
#define RELEASE_CALL_MEM_SEND RELEASE_CALL_MEM
#define RELEASE_CALL_MEM_RECV RELEASE_CALL_MEM

#define GRAB_RETURN_MEM_SEND do {\
	LOCK_RETURN_MEM; \
	WAIT_RETURN_MEM_SEND; \
} while (0)
#define GRAB_RETURN_MEM_RECV do {\
	LOCK_RETURN_MEM; \
	WAIT_RETURN_MEM_RECV; \	
} while (0)
#define RELEASE_RETURN_MEM do { \
	NOTIFY_RETURN_MEM; \
	UNLOCK_RETURN_MEM; \
} while (0)
#define RELEASE_RETURN_MEM_SEND RELEASE_RETURN_MEM
#define RELEASE_RETURN_MEM_RECV RELEASE_RETURN_MEM

#define OCELOT_MSG_CAN_WRITE (1u << 0)
#define OCELOT_MSG_CAN_READ (1u << 1)
#define OCELOT_MSG_ERR (1u << 2)
#define OCELOT_MSG_ERR_TOOMANYEVENTS (OCELOT_MSG_ERR | (1u << 0))
#define OCELOT_MSG_ERR_UNKNOWN (OCELOT_MSG_ERR | (1u << 1))
#define OCELOT_MSG_END (1u << 3)

//shape of shared memory; used to determine
//the features of the shared memory block
//to allocate (how many fields, how big,
//how aligned...)

#define MAX_EVENTS_NUMBER 1000

typedef struct {
	pthread_mutex_t mutex;
	pthread_cond_t ready;
	uint16_t msg;
	uint64_t events_length;
	_T_ocelot_event events[MAX_EVENTS_NUMBER];
} _T_ocelot_shm_return;


typedef struct {
	pthread_mutex_t mutex;
	pthread_cond_t ready;
	uint16_t msg;
	uint64_t values_length;
	uint64_t arrays_length;
	uint64_t pointers_length;
	uint64_t values_ofst;
	uint64_t arrays_ofst;
	uint64_t pointers_ofst;
} _T_ocelot_shm_call_header;

//local views of shared memory; used in local
//(nonshared) memory to point to shared memory
//structures to make the access to them easier.

typedef struct {
	pthread_mutex_t *mutex;
	pthread_cond_t *ready;
	uint16_t *msg;
	size_t *events_length;
	_T_ocelot_event *events;
} _T_ocelot_view_shm_return;

typedef struct {
	pthread_mutex_t *mutex;
	pthread_cond_t *ready;
	uint16_t *msg;
	size_t *values_length;
	size_t *arrays_length;
	size_t *pointers_length;
	size_t *values_ofst;
	size_t *arrays_ofst;
	size_t *pointers_ofst;
	double *values;
	double *arrays;
	double *pointers;
} _T_ocelot_view_shm_call;

//safety flag (used by parent)
static bool inactive = false;

//stop flag (used by child)
static volatile sig_atomic_t stop = 0;

//logger files
#ifdef LOG
static int fd_logger_parent;
static int fd_logger_child;
#endif

//pids of the children process
static pid_t pid_children[OCELOT_CORES];

//identifiers for shared memory
static int shm_id_return[OCELOT_CORES];
static int shm_id_call[OCELOT_CORES];

//pointers to shared memory (base address)
static void *shm_ptr_return[OCELOT_CORES];
static void *shm_ptr_call[OCELOT_CORES];

//maximum number of values, maximum number of arrays,
//maximum number of pointers
static size_t max_values_length;
static size_t max_arrays_length;
static size_t max_pointers_length;

//size of the three segments of shm_call 
//(in bytes)
static size_t values_size;
static size_t arrays_size;
static size_t pointers_size;

//offsets of the three segments of shm_call
//(in bytes)
static size_t values_ofst;
static size_t arrays_ofst;
static size_t pointers_ofst;

//total size of shm_return and shm_call 
//(in bytes)
static size_t shm_return_size;
static size_t shm_call_size;

//private functions forward declarations

static void _f_ocelot_throw_runtimeexception(JNIEnv *, char *);
static void _f_ocelot_transmit_events_to_java(JNIEnv *, jobject, size_t);

static void _f_ocelot_debug(char *, int);
static void _f_ocelot_memory_dump(size_t);
static ssize_t _f_ocelot_writeall(int, const void *, size_t);

//parent functions

static size_t _f_ocelot_align_up(size_t, size_t);
static int _f_ocelot_make_shm(size_t);
static int _f_ocelot_alloc_shm(size_t);
static int _f_ocelot_view_shm_return_init(_T_ocelot_view_shm_return *, size_t);
static int _f_ocelot_view_shm_call_init(_T_ocelot_view_shm_call *, size_t);
static void _f_ocelot_destroy_shm(size_t);

static int _f_ocelot_init_sync(size_t);
static void _f_ocelot_end_sync(size_t);

static int _f_ocelot_ensure_child(size_t);
static void _f_ocelot_end_child(size_t);

static void _f_ocelot_shutdown();

//common functions (parent and child)

static int _f_ocelot_lock_shm_return(_T_ocelot_view_shm_return *);
static int _f_ocelot_lock_shm_call(_T_ocelot_view_shm_call *);
static int _f_ocelot_unlock_shm_return(_T_ocelot_view_shm_return *);
static int _f_ocelot_unlock_shm_call(_T_ocelot_view_shm_call *);
static int _f_ocelot_wait_shm_return(_T_ocelot_view_shm_return *, bool);
static int _f_ocelot_wait_shm_call(_T_ocelot_view_shm_call *, bool);
static int _f_ocelot_notify_shm_return(_T_ocelot_view_shm_return *);
static int _f_ocelot_notify_shm_call(_T_ocelot_view_shm_call *);

//child functions

static void _f_ocelot_child_main(size_t);
static void _f_ocelot_init_arrays(double *, int, char *[]);
static void _f_ocelot_end_arrays(void);


/****************************************************************************************
 * SHARED MEMORIES
 *
 * RETURN
 *  ____ _____________
 * |SIZE|   EVENTS    |
 *  ¯¯¯¯ ¯¯¯¯¯¯¯¯¯¯¯¯¯
 *  SIZE: Number of events (max: MAX_EVENTS_NUMBER)
 *  EVENTS: Array of events (length: MAX_EVENTS_NUMBER)
 *
 * CALL
 *  ______ _____________ _____________ _______________ ___________ ___________ _____________ _____________
 * |SIGNAL|VALUES_LENGTH|ARRAYS_LENGTH|POINTERS_LENGTH|VALUES_OFST|ARRAYS_OFST|POINTERS_OFST|    DATA     |
 *  ¯¯¯¯¯¯ ¯¯¯¯¯¯¯¯¯¯¯¯¯ ¯¯¯¯¯¯¯¯¯¯¯¯¯ ¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯ ¯¯¯¯¯¯¯¯¯¯¯ ¯¯¯¯¯¯¯¯¯¯¯ ¯¯¯¯¯¯¯¯¯¯¯¯¯ ¯¯¯¯¯¯¯¯¯¯¯¯¯
 *
 *  SIGNAL: 0 -> Not ready; 1 -> Call can be performed; 2 -> All completed
 *  VALUES_LENGTH: Number of values (each value is a double)
 *  ARRAYS_LENGTH: Number of arrays (each array is a double[OCELOT_ARRAY_LENGTH])
 *  POINTERS_LENGTH: Number of pointers (each pointer is a double)
 *  VALUES_OFST: Offset of array of values in DATA (in bytes, starting from the base address of the call memory)
 *  ARRAYS_OFST: Offset of array of arrays in DATA (in bytes, starting from the base address of the call memory)
 *  POINTERS_OFST: Offset of array of pointers (in bytes, starting from the base address of the call memory)
 *  DATA: All the data: Array of values, then array of arrays, then array of pointers
 ***************************************************************************************/

 /*
 * Class:     it/unisa/ocelot/simulator/CBridge
 * Method:    privInit
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_it_unisa_ocelot_simulator_CBridge_privInit
		(JNIEnv *env, jclass cbridge, jint values, jint arrays, jint pointers) {
	//safety guard	
	if (inactive) {
		char exc_info[TXT_BUF_SIZE];
		sprintf(exc_info, "CBridge.getEvents: This CBridge is inactive (failed)");
		_f_ocelot_throw_runtimeexception(env, exc_info);
		return;
	}
	
	_f_ocelot_debug("Got call to CBridge.privInit\n", NODEBUGINFO);
	
	//initializes the parent logger
	LOG_PARENT_OPEN;

	LOG_MESSAGE("Logger session started.\n", fd_logger_parent);
	LOG_SEPARATOR(fd_logger_parent);
	
	//initializes all the arrays for bookkeeping children processes
	for (size_t i = 0; i < OCELOT_CORES; ++i) {
		pid_children[i] = NO_PID;
		shm_id_return[i] = NO_SHM_ID;
		shm_id_call[i] = NO_SHM_ID;
		shm_ptr_return[i] = SHM_FAILED;
		shm_ptr_call[i] = SHM_FAILED;
	}
	
	shm_return_size = sizeof(_T_ocelot_shm_return);
	
	max_values_length = (size_t) values;
	max_arrays_length = (size_t) arrays;
	max_pointers_length = (size_t) pointers;
	
	values_size = max_values_length * sizeof(double);
	arrays_size = max_arrays_length * OCELOT_ARRAY_LENGTH * sizeof(double);
	pointers_size = max_pointers_length * sizeof(double);
	
	size_t ofst = _f_ocelot_align_up(sizeof(_T_ocelot_shm_call_header), alignof(double));
	values_ofst = ofst;
	ofst += values_size;
	
	ofst = _f_ocelot_align_up(ofst, alignof(double));
	arrays_ofst = ofst;
	ofst += arrays_size;

	ofst = _f_ocelot_align_up(ofst, alignof(double));
	pointers_ofst = ofst;
	ofst += pointers_size;
	
	shm_call_size = ofst;
	_f_ocelot_debug("Calculated shared memory offsets\n", NODEBUGINFO);

	//prepares the shared memory for IPC
	for (size_t i = 0; i < OCELOT_CORES; ++i) {
		int error_code = _f_ocelot_make_shm(i);		
		if (error_code != ERROR_CODE_OK) {
			char exc_info[TXT_BUF_SIZE];
			if (error_code == ERROR_CODE_SHM_KEY) {
				sprintf(exc_info, "CBridge.privInit: Unable to create a shared memory key. Errno: %d", errno);
			} else if (error_code == ERROR_CODE_SHM_GET) {
				sprintf(exc_info, "CBridge.privInit: Unable to create shared memory. Errno: %d", errno);
			} else if (error_code == ERROR_CODE_SHM_ATTACH) {
				sprintf(exc_info, "CBridge.privInit: Unable to attach shared memory. Errno: %d", errno);
			} else if (error_code == ERROR_CODE_SHM_CTL) {
				sprintf(exc_info, "CBridge.privInit: Unable to control shared memory status. Errno: %d", errno);
			} else if (error_code == ERROR_CODE_SHM_NONE) {
				sprintf(exc_info, "CBridge.privInit: Unexpected error (suddenly there is no shared memory...)");
			} else if (error_code == ERROR_CODE_DATA_INVALID_RETURN_LENGTH) {
				sprintf(exc_info, "CBridge.privInit: Unexpected error (apparently the OS didn't zero the shared receive memory)");
			} else if (error_code == ERROR_CODE_DATA_INVALID_CALL_LENGTH) {
				sprintf(exc_info, "CBridge.privInit: Unexpected error (apparently the OS didn't zero the shared call memory)");
			} else { //unexpected error_code
				sprintf(exc_info, "CBridge.privInit: Unexpected error while attempting to create shared memory (error code: %d). Errno: %d", error_code, errno);
			}
			_f_ocelot_throw_runtimeexception(env, exc_info);
			goto fail;
		}
	}
	_f_ocelot_debug("Created shared memory\n", NODEBUGINFO);
	
	//success
	_f_ocelot_debug("CBridge.privInit ends successfully\n", NODEBUGINFO);
	return;
	
fail:
	_f_ocelot_shutdown();
	_f_ocelot_debug("CBridge.privInit ends with errors\n", NODEBUGINFO);
}

/*
* Class:     it/unisa/ocelot/simulator/CBridge
* Method:    getEvents
* Signature: (Lit/unisa/ocelot/simulator/EventsHandler;[D[[D[D)V
*/
JNIEXPORT void JNICALL Java_it_unisa_ocelot_simulator_CBridge_getEvents
		(JNIEnv *env, jobject self,
				jobject eventHandler, jdoubleArray values, jobjectArray arrays, jdoubleArray pointers) {
	if (inactive) {
		char exc_info[TXT_BUF_SIZE];
		sprintf(exc_info, "CBridge.getEvents: This CBridge is inactive (failed)");
		_f_ocelot_throw_runtimeexception(env, exc_info);
		return;
	}
	
	//gets this.coreId
	jclass cbridge = (*env)->GetObjectClass(env, self);
	jfieldID coreIdField = (*env)->GetFieldID(env, cbridge, "coreId", "I");
	jint coreId = (*env)->GetIntField(env, self, coreIdField);

	size_t i = (size_t) coreId; //index to current global arrays

	_f_ocelot_debug("PARENT: Got call of CBridge.getEvents, will interact with CHILD %zu\n", i);

	//gets the lengths of values, arrays, and pointers
	jsize lenValues = (*env)->GetArrayLength(env, values);
	jsize lenArrays = (*env)->GetArrayLength(env, arrays);
	jsize lenPointers = (*env)->GetArrayLength(env, pointers);

	//checks the lengths
	if (lenValues < 0 || lenValues > max_values_length ||
	    lenArrays < 0 || lenArrays > max_arrays_length ||
	    lenPointers < 0 || lenPointers > max_pointers_length) {
		char exc_info[TXT_BUF_SIZE];
		sprintf(exc_info, "CBridge.getEvents: The length of double[] values, or of double[][] arrays, or of double[] pointers is greater than the value provided during initialization");
		_f_ocelot_throw_runtimeexception(env, exc_info);
		return;
	}
	
	//converts the lengths
	size_t values_length = (size_t) lenValues;
	size_t arrays_length = (size_t) lenArrays;
	size_t pointers_length = (size_t) lenPointers;
		
	//creates the child process if it doesn't already exist
	_f_ocelot_debug("PARENT: Ensuring the presence of CHILD %zu...\n", i);
	int error_code = _f_ocelot_ensure_child(i);
	if (error_code != ERROR_CODE_OK) {
		char exc_info[TXT_BUF_SIZE];
		if (error_code == ERROR_CODE_FORK_TEMP) {
			sprintf(exc_info, "CBridge.getEvents: Too many attempts creating CHILD %zu. Errno: %d", i, errno);
		} else if (error_code == ERROR_CODE_FORK_NO_MEMORY) {
			sprintf(exc_info, "CBridge.getEvents: Cannot create CHILD %zu because memory exhausted. Errno: %d", i, errno);
		} else if (error_code == ERROR_CODE_UNEXPECTED) {
			sprintf(exc_info, "CBridge.getEvents: Unexpected errno while attempting to ensure CHILD %zu. Errno: %d", i, errno);
		} else { //unexpected error_code
			//This should never happen
			sprintf(exc_info, "CBridge.getEvents: Unexpected error while attempting to ensure CHILD %zu (error code: %d). Errno: %d", i, error_code, errno);
		}
		_f_ocelot_throw_runtimeexception(env, exc_info);
		return;
	}
	_f_ocelot_debug("PARENT: Ensured the presence of CHILD %zu\n", i);

	//Gets a local view of the call memory
	_T_ocelot_view_shm_call v_call;
	_f_ocelot_view_shm_call_init(&v_call, i);

	//Grabs the call memory to send to the i-th child
	_f_ocelot_debug("PARENT: Grabbing call memory to send to CHILD %zu...\n", i);
	GRAB_CALL_MEM_SEND;
	_f_ocelot_debug("PARENT: Grabbed call memory to send to CHILD %zu\n", i);

	//Sets the call memory with the lengths of values, arrays, and pointers
	SETVALUESLENGTH(values_length);
	SETARRAYSLENGTH(arrays_length);
	SETPOINTERSLENGTH(pointers_length);

	//Sets the call memory with the content of values
	(*env)->GetDoubleArrayRegion(env, values, 0, (jsize) values_length, (jdouble *) VALUES);
	
	//Sets the call memory with the content of arrays
	for (size_t k = 0; k < arrays_length; ++k) {
		jdoubleArray currentArray = (jdoubleArray) (*env)->GetObjectArrayElement(env, arrays, (jsize) k);
		(*env)->GetDoubleArrayRegion(env, currentArray, 0, (jsize) OCELOT_ARRAY_LENGTH, ARRAY(k));
	}

	//Sets the call memory with the content of pointers
	(*env)->GetDoubleArrayRegion(env, pointers, 0, (jsize) pointers_length, (jdouble *) POINTERS);

	//Sets the call memory message
	SETCALLMSG(OCELOT_MSG_CAN_READ);
	
	//Done
	_f_ocelot_debug("PARENT: Releasing call memory to send to CHILD %zu...\n", i);
	RELEASE_CALL_MEM_SEND;
	_f_ocelot_debug("PARENT: Released call memory to send to CHILD %zu\n", i);

	//Checks status of child - can it answer?
	int status;
	pid_t pid_child = waitpid(pid_children[i], &status, WNOHANG);
	if (pid_child == -1) {
		//error
		char exc_info[TXT_BUF_SIZE];
		if (errno == ECHILD) {
			sprintf(exc_info, "CBridge.getEvents: CHILD %zu does not exist", i);
		} else if (errno == EINTR) {
			sprintf(exc_info, "CBridge.getEvents: PARENT was interrupted by a signal while polling CHILD %zu status", i);
		} else if (errno == EINVAL) {
			sprintf(exc_info, "CBridge.getEvents: Unexpected error while PARENT was polling CHILD %zu status (invalid parameters to waitpid)", i);
		} else {
			//this should never happen
			sprintf(exc_info, "CBridge.getEvents: Unexpected error while PARENT was polling CHILD %zu status. Errno: ", i, errno);
		}		
		_f_ocelot_throw_runtimeexception(env, exc_info);
		goto fail;
	} else if (pid_child == pid_children[i]) {
		char exc_info[TXT_BUF_SIZE];
		if (WIFEXITED(status)) {
			int exit_status = WEXITSTATUS(status);
			sprintf(exc_info, "CBridge.getEvents: CHILD %zu process exited unexpectedly. Exit status: %d", i, exit_status);
		} else if (WIFSIGNALED(status)) {
			int term_signal = WTERMSIG(status);
			if (WCOREDUMP(status)) {
				sprintf(exc_info, "CBridge.getEvents: CHILD %zu process terminated by a signal and attempted to produce a core dump. Signal: %d", i, term_signal);
			} else {
				sprintf(exc_info, "CBridge.getEvents: CHILD %zu process terminated by a signal. Signal: %d", i, term_signal);
			}
		} else if (WIFSTOPPED(status))  {
			int stop_signal = WSTOPSIG(status);
			sprintf(exc_info, "CBridge.getEvents: CHILD %zu process stopped by a signal. Signal: %d", i, stop_signal);
		} else if (WIFCONTINUED(status))  {
			sprintf(exc_info, "CBridge.getEvents: CHILD %zu process continued", i);
		} else {
			sprintf(exc_info, "CBridge.getEvents: CHILD %zu process changed status, but I cannot detect why. Status: %d", i, status);
		}
		_f_ocelot_throw_runtimeexception(env, exc_info);
		goto fail;
	}
	//Apparently the child is working

	//Gets a local view of the return memory
	_T_ocelot_view_shm_return v_return;
	_f_ocelot_view_shm_return_init(&v_return, i);
	
	//Grabs the return memory to receive from the i-th child
	_f_ocelot_debug("PARENT: Grabbing return memory to receive from CHILD %zu...\n", i);
	GRAB_RETURN_MEM_RECV;
	_f_ocelot_debug("PARENT: Grabbed return memory to receive from CHILD %zu\n", i);
	
	uint16_t msg = GETRETURNMSG;
	if (msg == OCELOT_MSG_CAN_READ) {
		//The child answered: transmit all the events to Java
		_f_ocelot_transmit_events_to_java(env, eventHandler, i);
		_f_ocelot_debug("PARENT: Events transmitted to Java\n", NODEBUGINFO);
		_f_ocelot_debug("PARENT: Done\n", NODEBUGINFO);
	} else {
		char exc_info[TXT_BUF_SIZE];
		if (msg == OCELOT_MSG_ERR_TOOMANYEVENTS) {
			sprintf(exc_info, "CBridge.getEvents: Events overflow from CHILD %zu. Please, ensure that there is no infinite loop; possibly increase MAX_EVENTS_NUMBER to solve this", i);
		} else if (msg == OCELOT_MSG_ERR_UNKNOWN) {
			sprintf(exc_info, "CBridge.getEvents: Unexpected error in native code");
		} else { //unrecognized msg
			//This should never happen
			sprintf(exc_info, "CBridge.getEvents: Unexpected error message from CHILD %zu (error code: %d)", i, msg);
		}
		_f_ocelot_throw_runtimeexception(env, exc_info);
		goto fail;
	}
	
	//Sets the return memory message
	SETRETURNMSG(OCELOT_MSG_CAN_WRITE);
	
	//Done
	_f_ocelot_debug("PARENT: Releasing return memory to receive from CHILD %zu...\n", i);
	RELEASE_RETURN_MEM_RECV;
	_f_ocelot_debug("PARENT: Released return memory to receive from CHILD %zu\n", i);
	
	//success
	_f_ocelot_debug("PARENT: CBridge.getEvents ends successfully\n", NODEBUGINFO);
	return;
	
fail:
	_f_ocelot_shutdown();
	_f_ocelot_debug("CBridge.getEvents ends with errors\n", NODEBUGINFO);
}

/*
 * Class:     it/unisa/ocelot/simulator/CBridge
 * Method:    memoryDump
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_it_unisa_ocelot_simulator_CBridge_memoryDump(JNIEnv *env, jobject self) {
	//safety guard	
	if (inactive) {
		char exc_info[TXT_BUF_SIZE];
		sprintf(exc_info, "CBridge.memoryDump: This CBridge is inactive (failed)");
		_f_ocelot_throw_runtimeexception(env, exc_info);
		return;
	}
	
	jclass cbridge = (*env)->GetObjectClass(env, self);

	//gets the coreId
	jfieldID coreIdField = (*env)->GetFieldID(env, cbridge, "coreId", "I");
	jint coreId = (*env)->GetIntField(env, self, coreIdField);

	size_t i = (size_t) coreId; //index to current global arrays
	
	_f_ocelot_memory_dump(i);
}

JNIEXPORT void JNICALL Java_it_unisa_ocelot_simulator_CBridge_shutdown(JNIEnv *env, jclass cbridge) {
	_f_ocelot_shutdown();
}


//private functions

static void _f_ocelot_throw_runtimeexception(JNIEnv *env, char *exc_info) {
	jclass exClass = (*env)->FindClass(env, "java/lang/RuntimeException");
	if (exClass == NULL) {
		return;
	}
	(*env)->ThrowNew(env, exClass, exc_info);
}

static void _f_ocelot_transmit_events_to_java(JNIEnv *env, jobject eventHandler, size_t i) {
	//Gets the EventHandler.add and EventHandler.addCase methods
	jclass eventHandlerClass = (*env)->GetObjectClass(env, eventHandler);
	jmethodID addMethod = (*env)->GetMethodID(env, eventHandlerClass, "add", "(IDD)V");
	jmethodID addCaseMethod = (*env)->GetMethodID(env, eventHandlerClass, "addCase", "(IDZ)V");
	
	_T_ocelot_view_shm_return v_return;
	_f_ocelot_view_shm_return_init(&v_return, i);

	size_t events_length = (size_t) GETEVENTSLENGTH;
	for (size_t k = 0; k < events_length; ++k) {
		LOG_EVENT(k, fd_logger_parent);
		if (GETEVENTKIND(k) == OCELOT_KIND_STDEV) {
			(*env)->CallVoidMethod(env, eventHandler, addMethod, (jint) GETEVENTSTDCHOICE(k), (jdouble) GETEVENTSTDDISTANCETRUE(k), (jdouble) GETEVENTSTDDISTANCEFALSE(k));
		} else if (GETEVENTKIND(k) == OCELOT_KIND_CASEV) {
			(*env)->CallVoidMethod(env, eventHandler, addCaseMethod, (jint) GETEVENTCASECHOICE(k), (jdouble) GETEVENTCASEDISTANCE(k), (jboolean) GETEVENTCASECHOSEN(k));
		} else {
			//this should never happen
		}
	}
	LOG_SEPARATOR(fd_logger_parent);
}

static void _f_ocelot_debug(char *info, int num) {
#ifdef DEBUG
	if (num == NODEBUGINFO) {
		fprintf(stderr, info);
	} else {
		fprintf(stderr, info, num);
	}
#endif
}

static void _f_ocelot_memory_dump(size_t i) {
	//gets view of call memory 
	_T_ocelot_view_shm_call v_call;
	_f_ocelot_view_shm_call_init(&v_call, i);

	fprintf(stderr, "------------ START MEMORY DUMP ------------\n");
	fprintf(stderr, "*************** CALL MEMORY ***************\n");
	fprintf(stderr, "MESSAGE: ");
	switch (GETCALLMSG) {
	case OCELOT_MSG_CAN_WRITE:
		fprintf(stderr, "OCELOT_MSG_CAN_WRITE\n");
		break;
	case OCELOT_MSG_CAN_READ:
		fprintf(stderr, "OCELOT_MSG_CAN_READ\n");
		break;
	case OCELOT_MSG_ERR_TOOMANYEVENTS:
		fprintf(stderr, "OCELOT_MSG_ERR_TOOMANYEVENTS\n");
		break;
	case OCELOT_MSG_ERR_UNKNOWN:
		fprintf(stderr, "OCELOT_MSG_ERR_UNKNOWN\n");
		break;
	default:
		fprintf(stderr, "?\n");
		break;	
	}
	size_t values_length = (size_t) GETVALUESLENGTH;
	size_t arrays_length = (size_t) GETARRAYSLENGTH;
	size_t pointers_length = (size_t) GETPOINTERSLENGTH;
	fprintf(stderr, "VALUES_LENGTH: %zu\n", values_length);
	fprintf(stderr, "ARRAYS_LENGTH: %zu\n", arrays_length);
	fprintf(stderr, "POINTERS_LENGTH: %zu\n", pointers_length);
	fprintf(stderr, "VALUES:");
	for (size_t k = 0; k < values_length; ++k) {
		fprintf(stderr, " %f", GETVALUE(k));
	}
	fprintf(stderr, "\nARRAYS:\n");
	for (size_t j = 0; j < arrays_length; ++j) {
		fprintf(stderr, "\tArray #%zu: { ", j);
		for (size_t k = 0; k < OCELOT_ARRAY_LENGTH; ++k) {
			fprintf(stderr, "%f ", GETARRAY(j, k));
		}
		fprintf(stderr, "}\n");
	}
	fprintf(stderr, "POINTERS:");
	for (size_t k = 0; k < pointers_length; ++k) {
		fprintf(stderr, " %f", GETPOINTER(k));
	}

	//gets view of return memory 
	_T_ocelot_view_shm_return v_return;
	_f_ocelot_view_shm_return_init(&v_return, i);
	fprintf(stderr, "\n************** RETURN MEMORY **************\n");
	fprintf(stderr, "MESSAGE: ");
	switch (GETRETURNMSG) {
	case OCELOT_MSG_CAN_WRITE:
		fprintf(stderr, "OCELOT_MSG_CAN_WRITE\n");
		break;
	case OCELOT_MSG_CAN_READ:
		fprintf(stderr, "OCELOT_MSG_CAN_READ\n");
		break;
	case OCELOT_MSG_ERR_TOOMANYEVENTS:
		fprintf(stderr, "OCELOT_MSG_ERR_TOOMANYEVENTS\n");
		break;
	case OCELOT_MSG_ERR_UNKNOWN:
		fprintf(stderr, "OCELOT_MSG_ERR_UNKNOWN\n");
		break;
	default:
		fprintf(stderr, "?\n");
		break;	
	}
	size_t events_length = (size_t) GETEVENTSLENGTH;
	fprintf(stderr, "EVENTS_LENGTH: %zu\n", events_length);
	fprintf(stderr, "EVENTS:\n");
	int fd_stderr = fileno(stderr);
	for (size_t j = 0; j < events_length; ++j) {
		LOG_EVENT(j, fd_stderr);
	}
	fprintf(stderr, "------------  END MEMORY DUMP  ------------\n");
}

static ssize_t _f_ocelot_writeall(int fd, const void *buf, size_t len) {
	size_t done = 0;
	while (done < len) {
	    ssize_t n = write(fd, buf + done, len - done);
	    if (n == -1) {
	        if (errno == EINTR) {
				//try again
				continue;
			}
	        return -1;
	    }
	    done += n;
	}
	return done;
}

static size_t _f_ocelot_align_up(size_t base, size_t next) {
    return (base + next - 1) & ~(next - 1);
}

static int _f_ocelot_make_shm(size_t i) {
	int error_code;
	int attempts_left = MAX_ATTEMPTS;
	do {
		--attempts_left;
		error_code = _f_ocelot_alloc_shm(i);
		if (error_code != ERROR_CODE_OK) {
			_f_ocelot_debug("Cannot get shared memory now, trying again later (sleeping...)\n", NODEBUGINFO);
			OCELOT_SLEEP;
		}
	} while (error_code != ERROR_CODE_OK && attempts_left > 0);

	if (error_code != ERROR_CODE_OK) {
		return error_code;
	}

	//initializes the return shared memory
	_T_ocelot_view_shm_return v_return;
	error_code = _f_ocelot_view_shm_return_init(&v_return, i);
	if (error_code != ERROR_CODE_OK) {
		return error_code;
	}
	SETRETURNMSG(OCELOT_MSG_CAN_WRITE);
	SETEVENTSLENGTH(0);

	//initializes the call shared memory
	_T_ocelot_view_shm_call v_call;
	error_code = _f_ocelot_view_shm_call_init(&v_call, i);
	if (error_code != ERROR_CODE_OK) {
		return error_code;
	}
	SETCALLMSG(OCELOT_MSG_CAN_WRITE);
	SETVALUESLENGTH(0);
	SETARRAYSLENGTH(0);
	SETPOINTERSLENGTH(0);
	SETVALUESOFST((uint64_t) values_ofst);
	SETARRAYSOFST((uint64_t) arrays_ofst);
	SETPOINTERSOFST((uint64_t) pointers_ofst);

	//initializes the synchronization primitives
	//that will be used to coordinate IPC
	//and control flow
	error_code = _f_ocelot_init_sync(i);
	return error_code;
}

static int _f_ocelot_alloc_shm(size_t i) {
	int error_code = ERROR_CODE_OK;
	
	if (shm_ptr_return[i] != SHM_FAILED) {
		//shm_return already allocated, start allocation of shm_call
		goto alloc_shm_call;
	}

	key_t shm_key_return = (int) (rand() * 9999);
	if (shm_key_return == IPC_PRIVATE) {
		error_code = ERROR_CODE_SHM_KEY;
		goto fail_alloc_shm_return;
	}

	shm_id_return[i] = shmget(shm_key_return, shm_return_size, IPC_CREAT|IPC_EXCL|0666);
	if (shm_id_return[i] == NO_SHM_ID) {
		error_code = ERROR_CODE_SHM_GET;
		goto fail_alloc_shm_return;
	}
	
	shm_ptr_return[i] = shmat(shm_id_return[i], NULL, 0);
	if (shm_ptr_return[i] == SHM_FAILED) {
		error_code = ERROR_CODE_SHM_ATTACH;
		goto fail_alloc_shm_return;
	}
	
	int shm_ctl_return = shmctl(shm_id_return[i], IPC_RMID, NULL);
	if (shm_ctl_return == -1) {
		error_code = ERROR_CODE_SHM_CTL;
		goto fail_alloc_shm_return;
	}
	
	//success: now, allocates shm_call (fall through)
	
alloc_shm_call:

	if (shm_ptr_call[i] != SHM_FAILED) {
		//shm_return already allocated
		goto finally; //nothing else to do
	}

	key_t shm_key_call = shm_key_return + 1;
	if (shm_key_call == IPC_PRIVATE) {
		error_code = ERROR_CODE_SHM_KEY;
		goto fail_alloc_shm_call;
	}
		
	shm_id_call[i] = shmget(shm_key_call, shm_call_size, IPC_CREAT|IPC_EXCL|0666);
	if (shm_id_call[i] == NO_SHM_ID) {
		error_code = ERROR_CODE_SHM_GET;
		goto fail_alloc_shm_call;
	}
		
	shm_ptr_call[i] = shmat(shm_id_call[i], NULL, 0);
	if (shm_ptr_call[i] == SHM_FAILED) {
		error_code = ERROR_CODE_SHM_ATTACH;
		goto fail_alloc_shm_call;
	}

	int shm_ctl_call = shmctl(shm_id_call[i], IPC_RMID, NULL);
	if (shm_ctl_call == -1) {
		error_code = ERROR_CODE_SHM_CTL;
		goto fail_alloc_shm_call;
	}
	
	goto finally; //success

fail_alloc_shm_return:
	if (shm_ptr_return[i] != SHM_FAILED) {
		shmdt(shm_ptr_return[i]);
		shm_ptr_return[i] = SHM_FAILED;
	}
	if (shm_id_return[i] != NO_SHM_ID) {
		shmctl(shm_id_return[i], IPC_RMID, NULL);
		shm_id_return[i] = NO_SHM_ID;
	}
	goto finally;
	
fail_alloc_shm_call:
	if (shm_ptr_call[i] != SHM_FAILED) {
		shmdt(shm_ptr_call[i]);
		shm_ptr_call[i] = SHM_FAILED;
	}
	if (shm_id_call[i] != NO_SHM_ID) {
		shmctl(shm_id_call[i], IPC_RMID, NULL);
		shm_id_call[i] = NO_SHM_ID;
	}
	//fall through

finally:
	return error_code;
}

/*
 * Initializes a _T_ocelot_view_shm_return.
 */
static int _f_ocelot_view_shm_return_init(_T_ocelot_view_shm_return *v_return_ptr, size_t i) {
	if (shm_ptr_return[i] == SHM_FAILED) {
		return ERROR_CODE_SHM_NONE;
	}
	_T_ocelot_shm_return *shm = shm_ptr_return[i];

	//checks shm->events_length (safety check for correct cast to size_t and
	// to avoid buffer overflows)
	if (shm->events_length > MAX_EVENTS_NUMBER) {
		_f_ocelot_debug("ERROR: The events_length field of a receive message with CHILD %zu is too high\n", i);
		return ERROR_CODE_DATA_INVALID_RETURN_LENGTH; //do not use invalid data
	}
	
	//inits
	v_return_ptr->mutex = &(shm->mutex);
	v_return_ptr->ready = &(shm->ready);
	v_return_ptr->msg = &(shm->msg);
	v_return_ptr->events_length = (size_t *) &(shm->events_length);
	v_return_ptr->events = (_T_ocelot_event *) &(shm->events);
	
	return ERROR_CODE_OK;
}

/*
 * Initializes a _T_ocelot_view_shm_call.
 */
static int _f_ocelot_view_shm_call_init(_T_ocelot_view_shm_call *v_call_ptr, size_t i) {
	if (shm_ptr_call[i] == SHM_FAILED) {
		return ERROR_CODE_SHM_NONE;
	}
	_T_ocelot_shm_call_header *shm = shm_ptr_call[i];
	uint8_t *shm_base = shm_ptr_call[i]; //pointer to raw bytes
	
	//checks shm->values_length, shm->arrays_length, 
	//and shm->pointers_length (safety check for 
	//correct cast to size_t and to avoid buffer overflows)
	if (shm->values_length > max_values_length ||
	    shm->arrays_length > max_arrays_length ||
	    shm->pointers_length > max_pointers_length) {
		_f_ocelot_debug("ERROR: The values_length, arrays_length, or pointers_length field of a call message with CHILD%zu is too high\n", i);
		return ERROR_CODE_DATA_INVALID_CALL_LENGTH; //do not use invalid data
	}
	
	//shm->values_ofst, shm->arrays_ofst and shm->pointers_ofst were
	//set upon creation of the shared memory area, and we assume
	//they are correct
	
	//inits
	v_call_ptr->mutex = &(shm->mutex);
	v_call_ptr->ready = &(shm->ready);
	v_call_ptr->msg = &(shm->msg);
	v_call_ptr->values_length = (size_t *) &(shm->values_length);
	v_call_ptr->arrays_length = (size_t *) &(shm->arrays_length);
	v_call_ptr->pointers_length = (size_t *) &(shm->pointers_length);
	v_call_ptr->values_ofst = (size_t *) &(shm->values_ofst);
	v_call_ptr->arrays_ofst = (size_t *) &(shm->arrays_ofst);
	v_call_ptr->pointers_ofst = (size_t *) &(shm->pointers_ofst);
	v_call_ptr->values = (double *) (shm_base + (size_t) shm->values_ofst);
	v_call_ptr->arrays = (double *) (shm_base + (size_t) shm->arrays_ofst);
	v_call_ptr->pointers = (double *) (shm_base + (size_t) shm->pointers_ofst);

	return ERROR_CODE_OK;
}

static void _f_ocelot_destroy_shm(size_t i) {
	_f_ocelot_end_sync(i);
	
	if (shm_ptr_return[i] != SHM_FAILED) {
		shmdt(shm_ptr_return[i]);
		shm_ptr_return[i] = SHM_FAILED;
	}
	if (shm_id_return[i] != NO_SHM_ID) {
		shmctl(shm_id_return[i], IPC_RMID, NULL);
		shm_id_return[i] = NO_SHM_ID;
	}
	if (shm_ptr_call[i] != SHM_FAILED) {
		shmdt(shm_ptr_call[i]);
		shm_ptr_call[i] = SHM_FAILED;
	}
	if (shm_id_call[i] != NO_SHM_ID) {
		shmctl(shm_id_call[i], IPC_RMID, NULL);
		shm_id_call[i] = NO_SHM_ID;
	}
}

static int _f_ocelot_init_sync(size_t i) {
	_T_ocelot_view_shm_call v_call;
	_T_ocelot_view_shm_return v_return;
	int error_code = _f_ocelot_view_shm_call_init(&v_call, i);
	if (error_code != ERROR_CODE_OK) {
		return error_code;
	}
	error_code = _f_ocelot_view_shm_return_init(&v_return, i);
	if (error_code != ERROR_CODE_OK) {
		return error_code;
	}
	
	pthread_mutexattr_t ma;
	pthread_mutexattr_init(&ma);
	pthread_mutexattr_setpshared(&ma, PTHREAD_PROCESS_SHARED);
	pthread_mutex_init(CALLMUTEX, &ma);
	pthread_mutex_init(RETURNMUTEX, &ma);
	pthread_mutexattr_destroy(&ma);
	pthread_condattr_t ca;
	pthread_condattr_init(&ca);
	pthread_condattr_setpshared(&ca, PTHREAD_PROCESS_SHARED);
	pthread_cond_init(CALLREADY, &ca);
	pthread_cond_init(RETURNREADY, &ca);
	pthread_condattr_destroy(&ca);

	return ERROR_CODE_OK;
}

static void _f_ocelot_end_sync(size_t i) {
	_T_ocelot_view_shm_call v_call;
	_T_ocelot_view_shm_return v_return;
	int error_code = _f_ocelot_view_shm_call_init(&v_call, i);
	if (error_code != ERROR_CODE_OK) {
		goto end_sync_return;
	}
	pthread_mutex_destroy(CALLMUTEX);
	pthread_cond_destroy(CALLREADY);

end_sync_return:
	error_code = _f_ocelot_view_shm_return_init(&v_return, i);
	if (error_code != ERROR_CODE_OK) {
		return;
	}
	pthread_mutex_destroy(RETURNMUTEX);
	pthread_cond_destroy(RETURNREADY);
}

/*
 * Forks and creates a child. This function describes the behavior of the child, 
 * who waits for a signal in call memory and, when received, starts the normal 
 * procedure.
 *
 * COMMENTS: we must take into account that, being the child process
 * a clone of the parent, it will execute its code in the context
 * of a (cloned) JVM. Unfortunately this JVM is in a highly 
 * incoherent state, because fork() does not clone the threads 
 * different from the one that invokes it - and the JVM has 
 * LOTS of threads for stuff like garbage collection, finalization, 
 * etc. The JVM also sets many things that are inherited by the
 * child process, e.g., signal handlers. This highly complex (and
 * corrupted) execution environment is challenging to manage. For
 * instance, if another thread in the parent JVM (either internal to
 * the JVM, or an application thread) was holding a 
 * lock, that lock will be forever locked in the child because 
 * since the holder thread is not cloned, noone will never release 
 * such lock. If the child process' threads attempt grabbing that lock
 * they will just deadlock. Also, if the thread that invokes fork()
 * has locks, it may end up locking for a long time the parent JVM
 * process.
 * There is also another downside: cloning a process that holds 
 * a lot of memory, as a whole JVM, could be stressful for the 
 * OS, which might end up failing to fork with ENOMEM.
 * 
 * So we need to attain to a couple of safety rules:
 * 
 * 1- NEVER return to the JVM. Always exit from the child 
 *    by terminating the process without returning.
 * 2- Do not do this by invoking exit(): invoke _exit(). This 
 *    because _exit() does not execute all the exit handlers 
 *    installed by the JVM has installed. Executing them is 
 *    HIGHLY dangerous because of the inconsistent state of
 *    the cloned JVM.
 * 3- NEVER use malloc. The JVM handles the heap segment, so 
 *    malloc could go bananas if called. For the same reason,
 *    NEVER attempt accessing a Java object with JNI: the 
 *    Java heap might also be in a highly inconsistent state,
 *    e.g. because fork() was invoked while the the GC thread
 *    was collecting. Also, in such situation it is possible
 *    that the GC thread was holding a lock, so invoking the
 *    JNI functions to access the Java heap memory could 
 *    cause your thread to deadlock.
 * 4- For the same reason NEVER use printf, or any stuff in
 *    stdio.h: they are protected by locks, that may also 
 *    end up being eternally locked in the child process.
 * 5- Deactivate signals: the JVM sets up a number of signal
 *    handlers, that shall NEVER be executed. Alternatively,
 *    register new handlers for the signals of interest.
 * 6- Close all inherited files that are not used by the child
 *    process, especially the files which the parent process
 *    locked with flock() (the record locks created with 
 *    fcntl(F_SETLK) are not inherited by the child process,
 *    so they are not problematic).
 * 7- Invoke only async-signal-safe APIs. This is the general
 *    POSIX rule for which APIs you may and may not invoke in 
 *    a child process between a fork() and an exec(). Here we 
 *    never invoke exec(), so the child process can only use
 *    async-signal-safe APIs for its whole life.
 * 8- If the child inherited some open FILE * or DIR *, these
 *    are unsafe to use, because they have buffers in userspace.
 *    These buffers are cloned, so if the parent thread had
 *    a (partially) filled userspace buffer, and after the 
 *    fork() both the parent and the child flush, this causes 
 *    a double write of the data in the buffer. Better to use
 *    the POSIX write() API, whose buffers are in the kernel 
 *    space, and which is async-signal-safe.  
 * 
 * In the end, the approach of cloning with fork(), and then
 * invoking the code under test by a plain C call, is very 
 * hackish and dangerous. Forking a whole JVM does not provide 
 * a nice environment where to run the code under test, 
 * especially if it calls non-async-signal-safe APIs (e.g., 
 * printf), or if it uses libraries that might do so. Perhaps 
 * in the future we should adopt the safer approach of compiling 
 * the code under test to a separate executable file, and then 
 * invoke exec() after fork, or even better, invoke posix_spawn(), 
 * that creates a child process directly executing a new binary, 
 * without cloning the parent.
 */
static int _f_ocelot_ensure_child(size_t i) {
	if (pid_children[i] != NO_PID) {
		//nothing to do: the i-th child process
		//already exists
		return ERROR_CODE_OK;
	}
	
	//makes a logger file for the child
	LOG_CHILD_OPEN(i);
	
	//blocks all the signals for the current thread 
	//before forking, so the child's thread will
	//also have all the signals blocked;
	//also saves the signal mask to reactivate
	//signals in the parent's thread after forking.
	sigset_t new, old;
	sigfillset(&new); //initializes new to a empty setmask
	pthread_sigmask(SIG_SETMASK, &new, &old);
	
	int error_code = ERROR_CODE_OK;
	int attempts_left = MAX_ATTEMPTS;
	do {
		--attempts_left;
		
		//forks
		pid_children[i] = fork();
	
		if (pid_children[i] == NO_PID) {
			//we are in the child process (i-th child)
			_f_ocelot_debug("CHILD %d: Forked\n", i);
			
			//starts the child code (never returns)
			_f_ocelot_child_main(i);
			
			//ensures the child never returns (safety net)
			_f_ocelot_debug("CHILD %d: SEVERE ERROR: Returned from entry point of child process. Abnormal exit\n", i);
			_exit(ERROR_CODE_UNEXPECTED);
		} else if (pid_children[i] < 0) {
			//we are in the parent process, error while forking
			
			//safety reset
			pid_children[i] = NO_PID;
				   		
			//sets error code
			if (errno == EAGAIN) {
				_f_ocelot_debug("ERROR: Cannot fork right now, trying again later (sleeping...)\n", i);
				error_code = ERROR_CODE_FORK_TEMP;
				OCELOT_SLEEP;
			} else if (errno == ENOMEM) {
				_f_ocelot_debug("ERROR: Cannot fork, not enough memory\n", i);
				error_code = ERROR_CODE_FORK_NO_MEMORY;
			} else {
				//this should never happen
				error_code = ERROR_CODE_UNEXPECTED;
			}
		} //else we are in the parent process, fork succeeded
	} while (error_code == ERROR_CODE_FORK_TEMP && attempts_left > 0);
	
	//we still are in the parent process, fork succeeded
	//or failed

	//closes the child logger
	LOG_CHILD_CLOSE;

	//reactivates signals
	pthread_sigmask(SIG_SETMASK, &old, NULL);

	//returns
	return error_code;
}

static void _f_ocelot_end_child(size_t i) {
	if (pid_children[i] == NO_PID) {
		return;
	}
		
	//Gets a local view of the call memory
	_T_ocelot_view_shm_call v_call;
	_f_ocelot_view_shm_call_init(&v_call, i);

	//Gets a local view of the return memory
	_T_ocelot_view_shm_return v_return;
	_f_ocelot_view_shm_return_init(&v_return, i);

	_f_ocelot_debug("PARENT: Grabbing call memory to send to CHILD %zu...\n", i);
	GRAB_CALL_MEM_SEND;
	_f_ocelot_debug("PARENT: Grabbed call memory to send to CHILD %zu\n", i);
	
	SETCALLMSG(OCELOT_MSG_END);
	
	_f_ocelot_debug("PARENT: Releasing call memory to send to CHILD %zu...\n", i);
	RELEASE_CALL_MEM_SEND;
	_f_ocelot_debug("PARENT: Released call memory to send to CHILD %zu\n", i);
	
	int status;
	pid_t pid_child = waitpid(pid_children[i], &status, 0);
	if (pid_child == -1) {
		//error
		if (errno == ECHILD) {
			_f_ocelot_debug("PARENT: CHILD %zu process does not exist\n", i);
		} else {
			//this should never happen
			kill(pid_child, SIGKILL);
		}		
	} else if (pid_child == pid_children[i]) {
		if (WIFEXITED(status)) {
			int exit_status = WEXITSTATUS(status);
			if (exit_status == ERROR_CODE_OK) {
				_f_ocelot_debug("PARENT: CHILD %zu process terminated correctly\n", i);
			} else {
				_f_ocelot_debug("PARENT: CHILD %zu process terminated incorrectly\n", i);
			}
		} else {
			_f_ocelot_debug("PARENT: CHILD %zu changed status but did not exit (killing child)\n", i);
			kill(pid_child, SIGKILL);
		}
	} else {
		_f_ocelot_debug("PARENT: Unexpected error while waiting for CHILD %zu\n", i);
		kill(pid_child, SIGKILL);
	}
		
	pid_children[i] = NO_PID;
}

void _f_ocelot_shutdown() {
	for (size_t i = 0; i < OCELOT_CORES; ++i) {
		_f_ocelot_end_child(i);
		_f_ocelot_destroy_shm(i);
	}
	LOG_PARENT_CLOSE;
	inactive = true;
}

//common functions (parent and child)

static int _f_ocelot_lock_shm_return(_T_ocelot_view_shm_return *v_return_ptr) {
	int error_code = pthread_mutex_lock(v_return_ptr->mutex);
	if (error_code != 0) {
		return ERROR_CODE_LOCK_FAILED;
	}
	
	return ERROR_CODE_OK;
}

static int _f_ocelot_lock_shm_call(_T_ocelot_view_shm_call *v_call_ptr) {
	int error_code = pthread_mutex_lock(v_call_ptr->mutex);
	if (error_code != 0) {
		return ERROR_CODE_LOCK_FAILED;
	}
	
	return ERROR_CODE_OK;
}

static int _f_ocelot_unlock_shm_return(_T_ocelot_view_shm_return *v_return_ptr) {
	int error_code = pthread_mutex_unlock(v_return_ptr->mutex);
	if (error_code != 0) {
		return ERROR_CODE_LOCK_FAILED;
	}
	
	return ERROR_CODE_OK;
}

static int _f_ocelot_unlock_shm_call(_T_ocelot_view_shm_call *v_call_ptr) {
	int error_code = pthread_mutex_unlock(v_call_ptr->mutex);
	if (error_code != 0) {
		return ERROR_CODE_LOCK_FAILED;
	}
	
	return ERROR_CODE_OK;
}

static int _f_ocelot_wait_shm_return(_T_ocelot_view_shm_return *v_return_ptr, bool send) {
	while (!(*(v_return_ptr->msg) & (send ? OCELOT_MSG_CAN_WRITE : OCELOT_MSG_CAN_READ)) &&
	       !(*(v_return_ptr->msg) & OCELOT_MSG_ERR) &&
	       !(*(v_return_ptr->msg) & OCELOT_MSG_END)) {
		int error_code = pthread_cond_wait(v_return_ptr->ready, v_return_ptr->mutex);
		if (error_code != 0) {
			return ERROR_CODE_WAIT_FAILED;
		}
	}
	
	return ERROR_CODE_OK;
}

static int _f_ocelot_wait_shm_call(_T_ocelot_view_shm_call *v_call_ptr, bool send) {
	while (!(*(v_call_ptr->msg) & (send ? OCELOT_MSG_CAN_WRITE : OCELOT_MSG_CAN_READ)) &&
	       !(*(v_call_ptr->msg) & OCELOT_MSG_ERR) &&
	       !(*(v_call_ptr->msg) & OCELOT_MSG_END)) {
		int error_code = pthread_cond_wait(v_call_ptr->ready, v_call_ptr->mutex);
		if (error_code != 0) {
			return ERROR_CODE_WAIT_FAILED;
		}
	}
	
	return ERROR_CODE_OK;
}

static int _f_ocelot_notify_shm_return(_T_ocelot_view_shm_return *v_return_ptr) {
	int error_code = pthread_cond_signal(v_return_ptr->ready);
	if (error_code != 0) {
		return ERROR_CODE_NOTIFY_FAILED;
	}

	return ERROR_CODE_OK;
}

static int _f_ocelot_notify_shm_call(_T_ocelot_view_shm_call *v_call_ptr) {
	int error_code = pthread_cond_signal(v_call_ptr->ready);
	if (error_code != 0) {
		return ERROR_CODE_NOTIFY_FAILED;
	}

	return ERROR_CODE_OK;
}

//child functions

static void _f_ocelot_child_main(size_t i) {
	//does some logging on own logger
	LOG_MESSAGE("Logger session started.\n", fd_logger_child);
	LOG_SEPARATOR(fd_logger_child);

	//sets the default signal handlers
	//for all signals
	struct sigaction sa = {0};
	sa.sa_handler = SIG_DFL;
	sigfillset(&sa.sa_mask); //block all signals while in the handler
	for (int s = 1; s < NSIG; s++) {
		if (s != SIGKILL && s != SIGSTOP && s != SIGTERM) {
			sigaction(s, &sa, NULL);
		}
	}

	_f_ocelot_debug("CHILD %zu: Set signal handlers\n", i);
	
	//reactivates all signals (especially SIGTERM)
	sigset_t empty_mask;
	sigemptyset(&empty_mask);
	pthread_sigmask(SIG_SETMASK, &empty_mask, NULL);
	_f_ocelot_debug("CHILD %zu: Signals reactivated\n", i);
	
	//gets the pid of the parent
	pid_t pid_parent = getppid();
	
	//will need it later
	char *types[] = OCELOT_TYPES;

	//Gets a local view of the call memory
	_T_ocelot_view_shm_call v_call;
	_f_ocelot_view_shm_call_init(&v_call, i);
	
	//Gets a local view of the return memory
	_T_ocelot_view_shm_return v_return;
	_f_ocelot_view_shm_return_init(&v_return, i);
	
	_f_ocelot_debug("CHILD %zu: Grabbing call memory to receive test inputs from PARENT...\n", i);
	GRAB_CALL_MEM_RECV;
	_f_ocelot_debug("CHILD %zu: Grabbed call memory to receive test inputs from PARENT\n", i);
	
	//Performs a continuous execution loop
	while (kill(pid_parent, 0) == 0) { //checks the status of the parent
		//Note: the check of parent status has to be performed right before the call. This is
		//because it could happen that the resources are released only because the parent
		//process died.
		if (GETCALLMSG == OCELOT_MSG_END) {
			_f_ocelot_debug("CHILD %zu: Received termination message from PARENT\n", i);
			_f_ocelot_debug("CHILD %zu: Releasing call memory to receive from PARENT...\n", i);
			RELEASE_CALL_MEM_RECV;
			_f_ocelot_debug("CHILD %zu: Released call memory to receive from PARENT\n", i);
			goto end;
		}
		
		size_t values_length = (size_t) GETVALUESLENGTH;
		size_t arrays_length = (size_t) GETARRAYSLENGTH;
		size_t pointers_length = (size_t) GETPOINTERSLENGTH;

		double *values = VALUES;
		double *arrays = ARRAYS;
		double *pointers = POINTERS;

		_f_ocelot_init();
		_f_ocelot_debug("CHILD %zu: Init of test execution performed\n", i);

		//TODO Move next line in parent: It is a VERY BAD idea to invoke malloc/free in the child process (see point 3 in the discussion of _f_ocelot_ensure_child).
		_f_ocelot_init_arrays(arrays, arrays_length, types);
		_f_ocelot_debug("CHILD %zu: Init of arrays performed\n", i);

		_f_ocelot_debug("CHILD %zu: Running test...\n", i);
		EXECUTE_OCELOT_TEST;
		_f_ocelot_debug("CHILD %zu: Test execution ended\n", i);
		
		SETCALLMSG(OCELOT_MSG_CAN_WRITE);
		
		_f_ocelot_debug("CHILD %zu: Releasing call memory to receive from PARENT...\n", i);
		RELEASE_CALL_MEM_RECV;
		_f_ocelot_debug("CHILD %zu: Released call memory to receive from PARENT\n", i);
		
		_f_ocelot_debug("CHILD %zu: Grabbing return memory to send results of test execution to PARENT...\n", i);
		GRAB_RETURN_MEM_SEND;
		_f_ocelot_debug("CHILD %zu: Grabbed return memory to send results of test execution to PARENT\n", i);
		
		if (OCLIST_LENGTH(_v_ocelot_events) > MAX_EVENTS_NUMBER) {
			_f_ocelot_debug("CHILD %zu: ERROR: too many events\n", i);
			SETRETURNMSG(OCELOT_MSG_ERR_TOOMANYEVENTS);
		} else {
			SETRETURNMSG(OCELOT_MSG_CAN_READ);
		}

		size_t events_length = OCLIST_LENGTH(_v_ocelot_events);
		SETEVENTSLENGTH(events_length);
		for (size_t k = 0; k < events_length; ++k) {
			_T_ocelot_event event = OCLIST_GET(_v_ocelot_events, k, _T_ocelot_event);
			memcpy(EVENT(k), &event, sizeof(_T_ocelot_event));
			LOG_EVENT(k, fd_logger_child);
		}
		_f_ocelot_debug("CHILD %zu: Completed setting return memory\n", i);
		LOG_SEPARATOR(fd_logger_child);
		
		//TODO Move next line in parent: It is a VERY BAD idea to invoke malloc/free in the child process (see point 3 in the discussion of _f_ocelot_ensure_child).
		_f_ocelot_end_arrays();
		_f_ocelot_end();
	
		_f_ocelot_debug("CHILD %zu: Releasing return memory to send to PARENT...\n", i);
		RELEASE_RETURN_MEM_SEND;
		_f_ocelot_debug("CHILD %zu: Released return memory to send to PARENT\n", i);
		
		_f_ocelot_debug("CHILD %zu: Grabbing call memory to receive test inputs from PARENT...\n", i);
		GRAB_CALL_MEM_RECV;
		_f_ocelot_debug("CHILD %zu: Grabbed call memory to receive test inputs from PARENT\n", i);
	}
	
	_f_ocelot_debug("CHILD %zu: PARENT state changed\n", i);
	
	_f_ocelot_debug("CHILD %zu: Releasing call memory to receive test inputs from PARENT...\n", i);
	RELEASE_CALL_MEM_RECV;
	_f_ocelot_debug("CHILD %zu: Releasing call memory to receive test inputs from PARENT\n", i);
	
	int error_code;
	if (errno == ESRCH) {
		_f_ocelot_debug("CHILD %zu: Parent has exited\n", i);
		goto end_ok;
	} else if (errno == EPERM) {
		//this should never happen
		_f_ocelot_debug("CHILD %zu: Unexpected error: I do not have the permission to send signals to the parent process\n", i);
		error_code = ERROR_CODE_UNEXPECTED;
		goto end;
	} else {
		//this should never happen
		_f_ocelot_debug("CHILD %zu: Unexpected error: Unexpected value for errno\n", i);
		error_code = ERROR_CODE_UNEXPECTED;
		goto end;
	}
	
end_ok:
	error_code = ERROR_CODE_OK;

end:
	_f_ocelot_debug("CHILD %zu: Shutting down\n", i);
	shmdt(shm_ptr_call[i]);
	shmdt(shm_ptr_return[i]);
	_exit(error_code);
}

#define OCELOT_ARRAY_TRANSFER(type) ((type *) (_v_ocelot_pointers[i]))[j] = (type) array[i * OCELOT_ARRAY_LENGTH + j]

static void _f_ocelot_init_arrays(double *array, int arrays_length, char *types[]) {
	_v_ocelot_pointers = (_T_ocelot_array *) malloc(sizeof(_T_ocelot_array) * arrays_length);
	
	for (int i = 0; i < arrays_length; i++) {
		char num_buf[TXT_BUF_SIZE];
		for (int j = 0; types[i][j] != ':' && types[i][j] != '\0'; ++j) {
			num_buf[j] = types[i][j];
			if (types[i][j + 1] == ':') {
				num_buf[j + 1] = '\0';
			}
		}
		int type = atoi(num_buf);
		for (int j = 0; j < OCELOT_ARRAY_LENGTH; ++j) {
			/*int index = i * OCELOT_ARRAY_LENGTH + j;*/

			/*int valueInt = (int) array[index];
			double valueDouble = array[index];
			char valueChar = (char) array[index];
			unsigned int valueUInt = (unsigned int) array[index];
			unsigned char valueUChar = (unsigned char) array[index];

			int *_v_ocelot_pointers_int = (int *) (_v_ocelot_pointers[i]);
			double *_v_ocelot_pointers_double = _v_ocelot_pointers[i];
			char *_v_ocelot_pointers_char = (char *) (_v_ocelot_pointers[i]);
			unsigned int *_v_ocelot_pointers_uint = (unsigned int *) (_v_ocelot_pointers[i]);
			unsigned char *_v_ocelot_pointers_uchar = (unsigned char *) (_v_ocelot_pointers[i]);*/
			

			switch (type) {
			case TYPE_INT_N:
				OCELOT_ARRAY_TRANSFER(int);
				break;
			case TYPE_SIGNED_INT_N:
				OCELOT_ARRAY_TRANSFER(signed int);
				break;
			case TYPE_UNSIGNED_INT_N:
				OCELOT_ARRAY_TRANSFER(unsigned int);
				break;
			case TYPE_SHORT_N:
				OCELOT_ARRAY_TRANSFER(short);
				break;
			case TYPE_SIGNED_SHORT_N:
				OCELOT_ARRAY_TRANSFER(signed short);
				break;
			case TYPE_UNSIGNED_SHORT_N:
				OCELOT_ARRAY_TRANSFER(unsigned short);
				break;
			case TYPE_LONG_N:
				OCELOT_ARRAY_TRANSFER(long);
				break;
			case TYPE_SIGNED_LONG_N:
				OCELOT_ARRAY_TRANSFER(signed long);
				break;
			case TYPE_UNSIGNED_LONG_N:
				OCELOT_ARRAY_TRANSFER(unsigned long);
				break;
			case TYPE_LONGLONG_N:
				OCELOT_ARRAY_TRANSFER(long long);
				break;
			case TYPE_SIGNED_LONGLONG_N:
				OCELOT_ARRAY_TRANSFER(signed long long);
				break;
			case TYPE_UNSIGNED_LONGLONG_N:
				OCELOT_ARRAY_TRANSFER(unsigned long long);
				break;
			case TYPE_CHAR_N:
				OCELOT_ARRAY_TRANSFER(char);
				break;
			case TYPE_SIGNED_CHAR_N:
				OCELOT_ARRAY_TRANSFER(signed char);
				break;
			case TYPE_UNSIGNED_CHAR_N:
				OCELOT_ARRAY_TRANSFER(unsigned char);
				break;
			case TYPE_BOOL_N:
				OCELOT_ARRAY_TRANSFER(bool);
				break;
			case TYPE_FLOAT_N:
				OCELOT_ARRAY_TRANSFER(float);
				break;
			case TYPE_DOUBLE_N:
				OCELOT_ARRAY_TRANSFER(double);
				break;
			case TYPE_LONGDOUBLE_N:
				OCELOT_ARRAY_TRANSFER(long double);
				break;
			default:
				//TODO report error
				OCELOT_ARRAY_TRANSFER(double);
				break;
			}
		}
	}
}

static void _f_ocelot_end_arrays(void) {
	free(_v_ocelot_pointers);
}
