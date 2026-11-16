#include "ocelot.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#include <fcntl.h>
#include <sys/stat.h>
#include <unistd.h>

#include "lists.h"
#include "pointers.h"

int fd_values;
int fd_errors_1;
int fd_errors_2;

_T_ocelot_list *_v_ocelot_events;
_T_ocelot_list *_v_ocelot_fcalls;
_T_ocelot_array *_v_ocelot_pointers;

void _f_ocelot_init() {
	fd_values = open("fitnessValues.txt", O_WRONLY|O_CREAT|O_TRUNC, S_IRUSR|S_IWUSR|S_IRGRP|S_IWGRP|S_IROTH|S_IWOTH);
	fd_errors_1 = open("fitnessErrors.txt", O_WRONLY|O_CREAT|O_TRUNC, S_IRUSR|S_IWUSR|S_IRGRP|S_IWGRP|S_IROTH|S_IWOTH);
	fd_errors_2 = open("fitnessErrors2.txt", O_WRONLY|O_CREAT|O_TRUNC, S_IRUSR|S_IWUSR|S_IRGRP|S_IWGRP|S_IROTH|S_IWOTH);
	
	_v_ocelot_events = OCLIST_ALLOC(_T_ocelot_event);
	_v_ocelot_fcalls = OCLIST_ALLOC(double);
}

void _f_ocelot_end() {
	close(fd_values);
	close(fd_errors_1);
	close(fd_errors_2);
	
	OCLIST_FREE(_v_ocelot_events);
	OCLIST_FREE(_v_ocelot_fcalls);
}

int _f_ocelot_branch_out(char *function_name, int node_id, int result, double distance_true, double distance_false) {
	char message[100]; //a buffer for sprintf
	
	//The following statement handles infeasible conditions, e.g. 
	//default case in switch statements involving enums with only 
	//two values.
	if (distance_true == 0.0 && distance_false == 0.0) {
	   distance_true = 1.0;
	}  

	//Writes to the values file
	sprintf(message, "%s;branch%d-true;%f\n", function_name, node_id, distance_true);
	write(fd_values, message, strlen(message));
	sprintf(message, "%s;branch%d-false;%f\n", function_name, node_id, distance_false);
	write(fd_values, message, strlen(message));
	
	//Checks sanity of distance_true and distance_false (first check).
	if ((distance_true == distance_false) || (((distance_true == 1.0) && (distance_false != 0.0)) || ((distance_false == 1.0) && (distance_true != 0.0))) || (!((distance_true == 0.0) || (distance_false == 0.0)))) {
		// Reports to the error file 1
		sprintf(message, "%s;branch%d-true;%f\n", function_name, node_id, distance_true);
		write(fd_errors_1, message, strlen(message));
		sprintf(message, "%s;branch%d-false;%f\n", function_name, node_id, distance_false);
		write(fd_errors_1, message, strlen(message));
	}  

	//Checks sanity of distance_true and distance_false (second check).
	if (distance_true < 0.0 || distance_true > 1.0 || distance_false < 0.0 || distance_false > 1.0) {
		// Reports to the error file 2
		sprintf(message, "%s;branch%d-true;%f\n", function_name, node_id, distance_true);
		write(fd_errors_2, message, strlen(message));
		sprintf(message, "%s;branch%d-false;%f\n", function_name, node_id, distance_false);
		write(fd_errors_2, message, strlen(message));
	} 

	return result;
}

int _f_ocelot_trace(int result, double distance_true, double distance_false) {
	_T_ocelot_event event;
	event.kind = OCELOT_KIND_STDEV;
	event.d.s.choice = (result == 0 ? 0 : 1);
	event.d.s.distance_true = distance_true;
	event.d.s.distance_false = distance_false;
	OCLIST_APPEND(_v_ocelot_events, event);
	return result;
}

int _f_ocelot_trace_case(int branch, double distance, int is_chosen) {
	_T_ocelot_event event;
	event.kind = OCELOT_KIND_CASEV;
	event.d.c.choice = branch;
	event.d.c.distance = distance;
	event.d.c.chosen = is_chosen;
	OCLIST_APPEND(_v_ocelot_events, event);
	return 0;
}

double _f_ocelot_reg_fcall_numeric(double fcall, int how_many) {
	for (int i = 0; i < how_many; i++) {
		OCLIST_APPEND(_v_ocelot_fcalls, fcall);
	}
	return 1.0;
}

double _f_ocelot_reg_fcall_pointer(void *fcall, int how_many) {
	for (int i = 0; i < how_many; i++) {
		OCLIST_APPEND(_v_ocelot_fcalls, *(double *) fcall);
	}
	return (double) *(double *) fcall;
}

double _f_ocelot_get_fcall() {
	if (OCLIST_LENGTH(_v_ocelot_fcalls) == 0) {
		//error: empty function queue!
		return 0.0;
	}
	double element = OCLIST_GET(_v_ocelot_fcalls, 0, double);
	OCLIST_SHIFT(_v_ocelot_fcalls);
	return element;
}

double _f_ocelot_eq_numeric(double op1, double op2) {
	double k = op2 - op1;
	double result;
	if (k == 0.0) {
		result = 0.0;
	} else {
		//result = k + OCELOT_K;
		k = fabs(k) + 1.0;
		result = k / (1.0 + k);
	}
	return result;
}

double _f_ocelot_eq_pointer(void *op1, void *op2) {
	int pos1 = _f_ocelot_pointertotab(op1);
	int pos2 = _f_ocelot_pointertotab(op2);
	return _f_ocelot_eq_numeric(pos1, pos2);
}

double _f_ocelot_neq_numeric(double op1, double op2) {
	double k = op2 - op1;
	double result;
	if (k == 0.0) {
		result = OCELOT_K;
	} else {
		result = 0.0;
	}
	return result;
}

double _f_ocelot_neq_pointer(void *op1, void *op2) {
	int pos1 = _f_ocelot_pointertotab(op1);
	int pos2 = _f_ocelot_pointertotab(op2);
	return _f_ocelot_neq_numeric(pos1, pos2);
}

double _f_ocelot_gt_numeric(double op1, double op2) {
	double k = op2 - op1;
	double result;
	if (k < 0.0) {
		result = 0.0;
	} else {
		//result = k + OCELOT_K;
		k = fabs(k) + 1.0;
		result = k / (1.0 + k);
	}
	return result;
}

double _f_ocelot_gt_pointer(void *op1, void *op2) {
	int pos1 = _f_ocelot_pointertotab(op1);
	int pos2 = _f_ocelot_pointertotab(op2);
	return _f_ocelot_gt_numeric(pos1, pos2);
}

double _f_ocelot_ge_numeric(double op1, double op2) {
	double k = op2 - op1;
	double result;
	if (k <= 0.0) {
		result = 0.0;
	} else {
		//result = k + OCELOT_K;
		k = fabs(k) + 1.0;
		result = k / (1.0 + k);
	}
	return result;
}

double _f_ocelot_ge_pointer(void *op1, void *op2) {
	int pos1 = _f_ocelot_pointertotab(op1);
	int pos2 = _f_ocelot_pointertotab(op2);
	return _f_ocelot_ge_numeric(pos1, pos2);
}

double _f_ocelot_lt_numeric(double op1, double op2) {
	return _f_ocelot_gt_numeric(op2, op1);
}

double _f_ocelot_lt_pointer(void *op1, void *op2) {
	return _f_ocelot_ge_pointer(op2, op1);
}

double _f_ocelot_le_numeric(double op1, double op2) {
	return _f_ocelot_ge_numeric(op2, op1);
}

double _f_ocelot_le_pointer(void *op1, void *op2) {
	return _f_ocelot_gt_pointer(op2, op1);
}

double _f_ocelot_and(double op1, double op2) {
	double result = (op1 + op2) / 2.0;
	return result;
}

double _f_ocelot_or(double op1, double op2) {
	if (op1 < op2) {
		return op1;
	} else {
		return op2;
	}
}

double _f_ocelot_istrue(double flag) {
	if (flag == 0.0) {
		return OCELOT_K;
	} else { //(flag == 1.0)
		return 0.0;
	}
}

double _f_ocelot_isfalse(double flag) {
	if (flag == 0.0) {
		return 0.0;
	} else { //(flag == 1.0)
		return OCELOT_K;
	}
}

int _f_ocelot_pointertotab(void *ptr) {
	int result = (ptr - (void *) _v_ocelot_pointers) / sizeof(_T_ocelot_array);
	return result;
}
