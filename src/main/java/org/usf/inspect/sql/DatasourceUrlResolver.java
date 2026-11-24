package org.usf.inspect.sql;

import javax.sql.DataSource;

/**
 * 
 * @author u$f
 *
 */
public interface DatasourceUrlResolver {
    
	String resolve(DataSource ds);

}