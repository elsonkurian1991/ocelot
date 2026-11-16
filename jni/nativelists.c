#include "nativelists.h"

#include <stdlib.h>
#include <string.h>

//forward declarations
_T_ocelot_list_node *_f_ocelot_list_allocate_node(void *, size_t);
void _f_ocelot_list_free_node(_T_ocelot_list_node *);
_T_ocelot_list_node *_f_ocelot_list_last_node(_T_ocelot_list *);

_T_ocelot_list* _f_ocelot_list_allocate(size_t element_size) {
	_T_ocelot_list *list = (_T_ocelot_list *) malloc(sizeof(_T_ocelot_list));
	list->root = NULL;
	list->element_size = element_size;
	list->length = 0;
	return list;
}

void _f_ocelot_list_free(_T_ocelot_list *list) {
	if (list == NULL) {
		return;
	}
	
	_f_ocelot_list_free_node(list->root);
	
	free(list);
}

void _f_ocelot_list_append(_T_ocelot_list *list, void *value) {
	if (list == NULL) {
		return;
	}
	
	_T_ocelot_list_node *nnew = _f_ocelot_list_allocate_node(value, list->element_size);
	if (list->length == 0) {
		list->root = nnew;
	} else {
		_T_ocelot_list_node *nlast = _f_ocelot_list_last_node(list);
		nlast->next = nnew;
	}
	++(list->length);
}

void *_f_ocelot_list_get(_T_ocelot_list *list, size_t index) {
	if (list == NULL || index >= list->length) {
		return NULL;
	}

	_T_ocelot_list_node *node = list->root;
	for (size_t i = 0; i < index; ++i) {
		node = node->next;
	}

	return node->value;
}

void _f_ocelot_list_shift(_T_ocelot_list *list) {
	if (list == NULL || list->length == 0) {
		return;
	}

	_T_ocelot_list_node *prev_root = list->root;
	list->root = list->root->next;
	free(prev_root->value);
	free(prev_root);
	--(list->length);
}

_T_ocelot_list_node* _f_ocelot_list_allocate_node(void *value, size_t element_size) {
	_T_ocelot_list_node *node = (_T_ocelot_list_node *) malloc(sizeof(_T_ocelot_list_node));
	
	node->value = malloc(element_size);
	memcpy(node->value, value, element_size);
	node->next = NULL;
	
	return node;
}

void _f_ocelot_list_free_node(_T_ocelot_list_node *node) {
	if (node == NULL) {
		return;
	}
	
	free(node->value);
	_f_ocelot_list_free_node(node->next);
	
	free(node);
}

_T_ocelot_list_node *_f_ocelot_list_last_node(_T_ocelot_list *list) {
	if (list == null || list->length == 0) {
		return NULL;
	}

	_T_ocelot_list_node *node = list->root;
	while (node->next != NULL) {
		node = node->next;
	}
	
	return node;
}

