package it.unisa.ocelot.genetic.solutions;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
public interface CacheAccessor {
	  Object getCacheObject();
	  void setCacheObject(Object cacheObject);
}