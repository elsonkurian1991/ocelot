#ifndef _Included_OcelotListsHeader
#define _Included_OcelotListsHeader

/* Lists implemented by glib. The safety of using them in the 
 * context of a child processes is currently UNKNOWN!
 */

#include <glib.h>

#ifdef __cplusplus
extern "C" {
#endif

#define _T_ocelot_list GArray

#define OCLIST_ALLOC(type) g_array_new(FALSE, FALSE, sizeof(type))
#define OCLIST_FREE(list) g_array_free(list, TRUE)
#define OCLIST_APPEND(list, value) g_array_append_val(list, value)
#define OCLIST_GET(list, index, type) g_array_index(list, type, index)
#define OCLIST_LENGTH(list) ((size_t) list->len)
#define OCLIST_SHIFT(list) g_array_remove_index(list, 0)

#ifdef __cplusplus
}
#endif
#endif
