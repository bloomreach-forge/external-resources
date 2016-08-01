package org.onehippo.forge.externalresource.reports.plugins.statistics.list;

import org.json.JSONException;
import org.json.JSONObject;
import org.wicketstuff.js.ext.data.ExtDataField;
import org.wicketstuff.js.ext.form.ExtField;


/**
 * @version $Id$
 */
public interface IStatisticsListColumn<T> {

    public ExtDataField getExtField();

    public JSONObject getExtColumnConfig() throws JSONException;

    public String getValue(T statsItem);

    public String getName();

}
