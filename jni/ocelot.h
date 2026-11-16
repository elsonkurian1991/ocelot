#ifndef _Included_OcelotHeader
#define _Included_OcelotHeader

#include "lists.h"
#include "pointers.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifndef ABS
#define ABS(x) (x < 0 ? -x : x)
#endif

#define OCELOT_K 0.5
#define OCELOT_KIND_STDEV 1
#define OCELOT_KIND_CASEV 2

#define OCELOT_DEREF(type, variable) ((type) *((double *) variable))

typedef struct {
	int choice;
	double distance_true;
	double distance_false;
} _T_ocelot_event_data_stdev;

typedef struct {
	int choice;
	double distance;
	int chosen;
} _T_ocelot_event_data_casev;

typedef union {
	_T_ocelot_event_data_stdev s;
	_T_ocelot_event_data_casev c;
} _T_ocelot_event_data;

typedef struct {
	int kind;
	_T_ocelot_event_data d;
} _T_ocelot_event;

extern _T_ocelot_list *_v_ocelot_events;
extern _T_ocelot_list *_v_ocelot_fcalls;
extern _T_ocelot_array *_v_ocelot_pointers;

/*
 * Functions
 */

void _f_ocelot_init();
void _f_ocelot_end();

/*
 * Track the branches
 */
int _f_ocelot_branch_out(char *, int, int, double, double);

/*
 * Traces the branches
 */
int _f_ocelot_trace(int, double, double);
int _f_ocelot_trace_case(int, double, int);

/*
 * Functions that register in _v_ocelot_fcalls the result of each function call in the original expression.
 */
double _f_ocelot_reg_fcall_numeric(double, int);
double _f_ocelot_reg_fcall_pointer(void *, int);

double _f_ocelot_get_fcall();

int _f_ocelot_pointertotab(void *);

double _f_ocelot_eq_numeric(double, double);
double _f_ocelot_eq_pointer(void *, void *);

double _f_ocelot_neq_numeric(double, double);
double _f_ocelot_neq_pointer(void *, void *);

double _f_ocelot_gt_numeric(double, double);
double _f_ocelot_gt_pointer(void *, void *);

double _f_ocelot_ge_numeric(double, double);
double _f_ocelot_ge_pointer(void *, void *);

double _f_ocelot_lt_numeric(double, double);
double _f_ocelot_lt_pointer(void *, void *);

double _f_ocelot_le_numeric(double, double);
double _f_ocelot_le_pointer(void *, void *);

double _f_ocelot_and(double, double);
double _f_ocelot_or(double, double);

double _f_ocelot_istrue(double);
double _f_ocelot_isfalse(double);

#ifdef __cplusplus
}
#endif
#endif
