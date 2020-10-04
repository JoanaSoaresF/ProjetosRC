package cnss.simulator;

import java.util.HashMap;
import java.util.Map;

public class GlobalParameters {

	private Map<String, String> vars;

	/**
	 * <code>GlobalVars</code> constructor, maintains a map of global variables or parameters
	 */
	public GlobalParameters() {
		vars = new HashMap<>();
	}

	/**
	 * Inserts a new (parameter, value) pair in the map
	 * 
	 * @param name  of the parameter
	 * @param value its (new) value
	 */
	public void put(String name, String value) {
		vars.put(name, value);
	}

	/**
	 * Gets the value of a parameter from the map
	 * 
	 * @param name of the parameter 
	 * @return value its value
	 */
	public String get(String name) {
		return vars.get(name);
	}
	
	/**
	 * Returns true if a parameter belongs to the map, false otherwise
	 * 
	 * @param name of the parameter
	 * @return true if parameter belongs to the map
	 */
	public boolean containsKey(String name) {
		return vars.containsKey(name);
	}

	/**
	 * Generic toString method returning the contents of the mapping.
	 * 
	 * @return String
	 */
	public String toString() {
		return vars.toString();
	}

}
