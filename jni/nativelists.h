#ifndef _Included_OcelotNativeListsHeader
#define _Included_OcelotNativeListsHeader

/* Lists implemented with malloc. Potentially dangerous 
 * to be used by our child processes, depending on the 
 * libc implementation and os (so, not portable).
 */

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct _ocelot_list_node {
	void *value;
	struct _ocelot_list_node *next;
} _T_ocelot_list_node;

typedef struct {
	_T_ocelot_list_node *root;
	size_t element_size;
	size_t length;
} _T_ocelot_list;

#define OCLIST_ALLOC(type) _f_ocelot_list_allocate(sizeof(type))
#define OCLIST_FREE(list) _f_ocelot_list_free(list)
#define OCLIST_APPEND(list, value) _f_ocelot_list_append(list, &(value))
#define OCLIST_GET(list, index, type) *((type *) _f_ocelot_list_get(list, index))
#define OCLIST_LENGTH(list) (list->length)
#define OCLIST_SHIFT(list) _f_ocelot_list_shift(list)

_T_ocelot_list *_f_ocelot_list_allocate(size_t);
void _f_ocelot_list_free(_T_ocelot_list *);
void _f_ocelot_list_append(_T_ocelot_list *, void *);
void *_f_ocelot_list_get(_T_ocelot_list *, size_t);
void _f_ocelot_list_shift(_T_ocelot_list *);

#ifdef __cplusplus
}
#endif
#endif
