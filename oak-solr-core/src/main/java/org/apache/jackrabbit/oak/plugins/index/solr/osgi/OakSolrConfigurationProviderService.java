/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.plugins.index.solr.osgi;

import java.util.Arrays;
import java.util.Collection;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.commons.PropertiesUtil;
import org.apache.jackrabbit.oak.plugins.index.solr.configuration.CommitPolicy;
import org.apache.jackrabbit.oak.plugins.index.solr.configuration.OakSolrConfiguration;
import org.apache.jackrabbit.oak.plugins.index.solr.configuration.OakSolrConfigurationProvider;
import org.apache.jackrabbit.oak.plugins.index.solr.configuration.SolrServerConfigurationDefaults;
import org.apache.jackrabbit.oak.spi.query.Filter;
import org.osgi.service.component.ComponentContext;

/**
 * OSGi service for {@link org.apache.jackrabbit.oak.plugins.index.solr.configuration.OakSolrConfigurationProvider}
 */
@Component(label = "Oak Solr indexing / search configuration", metatype = true, immediate = true)
@Service(OakSolrConfigurationProvider.class)
public class OakSolrConfigurationProviderService implements OakSolrConfigurationProvider {

    private static final String DEFAULT_DESC_FIELD = SolrServerConfigurationDefaults.DESC_FIELD_NAME;
    private static final String DEFAULT_CHILD_FIELD = SolrServerConfigurationDefaults.CHILD_FIELD_NAME;
    private static final String DEFAULT_PARENT_FIELD = SolrServerConfigurationDefaults.ANC_FIELD_NAME;
    private static final String DEFAULT_PATH_FIELD = SolrServerConfigurationDefaults.PATH_FIELD_NAME;
    private static final String DEFAULT_CATCHALL_FIELD = SolrServerConfigurationDefaults.CATCHALL_FIELD;
    private static final int DEFAULT_ROWS = SolrServerConfigurationDefaults.ROWS;
    private static final boolean DEFAULT_PATH_RESTRICTIONS = SolrServerConfigurationDefaults.PATH_RESTRICTIONS;
    private static final boolean DEFAULT_PROPERTY_RESTRICTIONS = SolrServerConfigurationDefaults.PROPERTY_RESTRICTIONS;
    private static final boolean DEFAULT_PRIMARY_TYPES_RESTRICTIONS = SolrServerConfigurationDefaults.PRIMARY_TYPES;

    @Property(value = DEFAULT_DESC_FIELD, label = "field for descendants search")
    private static final String PATH_DESCENDANTS_FIELD = "path.desc.field";

    @Property(value = DEFAULT_CHILD_FIELD, label = "field for children search")
    private static final String PATH_CHILDREN_FIELD = "path.child.field";

    @Property(value = DEFAULT_PARENT_FIELD, label = "field for parent search")
    private static final String PATH_PARENT_FIELD = "path.parent.field";

    @Property(value = DEFAULT_PATH_FIELD, label = "field for path search")
    private static final String PATH_EXACT_FIELD = "path.exact.field";

    @Property(value = SolrServerConfigurationDefaults.CATCHALL_FIELD, label = "catch all field")
    private static final String CATCH_ALL_FIELD = "catch.all.field";

    @Property(options = {
            @PropertyOption(name = "HARD",
                    value = "Hard commit"
            ),
            @PropertyOption(name = "SOFT",
                    value = "Soft commit"
            ),
            @PropertyOption(name = "AUTO",
                    value = "Auto commit"
            )},
            value = "SOFT"
    )
    private static final String COMMIT_POLICY = "commit.policy";


    @Property(intValue = DEFAULT_ROWS, label = "rows")
    private static final String ROWS = "rows";

    @Property(boolValue = DEFAULT_PATH_RESTRICTIONS, label = "path restrictions")
    private static final String PATH_RESTRICTIONS = "path.restrictions";

    @Property(boolValue = DEFAULT_PROPERTY_RESTRICTIONS, label = "property restrictions")
    private static final String PROPERTY_RESTRICTIONS = "property.restrictions";

    @Property(boolValue = DEFAULT_PRIMARY_TYPES_RESTRICTIONS, label = "primary types restrictions")
    private static final String PRIMARY_TYPES_RESTRICTIONS = "primarytypes.restrictions";

    @Property(value = SolrServerConfigurationDefaults.IGNORED_PROPERTIES, label = "ignored properties",
            unbounded = PropertyUnbounded.ARRAY)
    private static final String IGNORED_PROPERTIES = "ignored.properties";

    @Property(value = SolrServerConfigurationDefaults.TYPE_MAPPINGS, cardinality = 13, description =
            "each item should be in the form TypeString=FieldName (e.g. STRING=text_general)", label =
            "mappings from Oak Types to Solr fields")
    private static final String TYPE_MAPPINGS = "type.mappings";

    @Property(value = SolrServerConfigurationDefaults.PROPERTY_MAPPINGS, unbounded = PropertyUnbounded.ARRAY, description =
            "each item should be in the form PropertyName=FieldName (e.g. jcr:title=text_en)", label =
            "mappings from JCR property names to Solr fields")
    private static final String PROPERTY_MAPPINGS = "property.mappings";

    private String pathChildrenFieldName;
    private String pathParentFieldName;
    private String pathDescendantsFieldName;
    private String pathExactFieldName;
    private String catchAllField;
    private CommitPolicy commitPolicy;
    private int rows;
    private boolean useForPathRestrictions;
    private boolean useForPropertyRestrictions;
    private boolean useForPrimaryTypes;
    private String[] ignoredProperties;
    private String[] typeMappings;
    private String[] propertyMappings;

    private OakSolrConfiguration oakSolrConfiguration;

    @Activate
    protected void activate(ComponentContext componentContext) throws Exception {
        pathChildrenFieldName = String.valueOf(componentContext.getProperties().get(PATH_CHILDREN_FIELD));
        pathParentFieldName = String.valueOf(componentContext.getProperties().get(PATH_PARENT_FIELD));
        pathExactFieldName = String.valueOf(componentContext.getProperties().get(PATH_EXACT_FIELD));
        pathDescendantsFieldName = String.valueOf(componentContext.getProperties().get(PATH_DESCENDANTS_FIELD));
        catchAllField = String.valueOf(componentContext.getProperties().get(CATCH_ALL_FIELD));
        rows = Integer.parseInt(String.valueOf(componentContext.getProperties().get(ROWS)));
        commitPolicy = CommitPolicy.valueOf(String.valueOf(componentContext.getProperties().get(COMMIT_POLICY)));
        useForPathRestrictions = Boolean.valueOf(String.valueOf(componentContext.getProperties().get(PATH_RESTRICTIONS)));
        useForPropertyRestrictions = Boolean.valueOf(String.valueOf(componentContext.getProperties().get(PROPERTY_RESTRICTIONS)));
        useForPrimaryTypes = Boolean.valueOf(String.valueOf(componentContext.getProperties().get(PRIMARY_TYPES_RESTRICTIONS)));
        typeMappings = PropertiesUtil.toStringArray(componentContext.getProperties().get(TYPE_MAPPINGS));
        ignoredProperties = PropertiesUtil.toStringArray(componentContext.getProperties().get(IGNORED_PROPERTIES));
        propertyMappings = PropertiesUtil.toStringArray(componentContext.getProperties().get(PROPERTY_MAPPINGS));
    }

    @Deactivate
    protected void deactivate() {
        oakSolrConfiguration = null;
    }

    @Override
    public OakSolrConfiguration getConfiguration() {
        if (oakSolrConfiguration == null) {
            oakSolrConfiguration = new OakSolrConfiguration() {

                @Override
                public String getFieldNameFor(Type<?> propertyType) {
                    for (String typeMapping : typeMappings) {
                        String[] mapping = typeMapping.split("=");
                        if (mapping.length == 2 && mapping[0] != null && mapping[1] != null) {
                            Type<?> type = Type.fromString(mapping[0]);
                            if (type != null && type.tag() == propertyType.tag()) {
                                return mapping[1];
                            }
                        }
                    }
                    return null;
                }

                @Override
                public String getFieldForPropertyRestriction(Filter.PropertyRestriction propertyRestriction) {
                    for (String propertyMapping : propertyMappings) {
                        String[] mapping = propertyMapping.split("=");
                        if (mapping.length == 2 && mapping[0] != null && mapping[1] != null) {
                            if (mapping[0].equals(propertyRestriction.propertyName)) {
                                return mapping[1];
                            }
                        }
                    }
                    return null;
                }

                @Override
                public String getPathField() {
                    return pathExactFieldName;
                }

                @Override
                public String getFieldForPathRestriction(Filter.PathRestriction pathRestriction) {
                    String fieldName = null;
                    switch (pathRestriction) {
                        case ALL_CHILDREN: {
                            fieldName = pathDescendantsFieldName;
                            break;
                        }
                        case DIRECT_CHILDREN: {
                            fieldName = pathChildrenFieldName;
                            break;
                        }
                        case EXACT: {
                            fieldName = pathExactFieldName;
                            break;
                        }
                        case PARENT: {
                            fieldName = pathParentFieldName;
                            break;
                        }
                        case NO_RESTRICTION:
                            break;
                        default:
                            break;

                    }
                    return fieldName;
                }

                @Override
                public CommitPolicy getCommitPolicy() {
                    return commitPolicy;
                }

                @Override
                public String getCatchAllField() {
                    return catchAllField;
                }

                @Override
                public int getRows() {
                    return rows;
                }

                @Override
                public boolean useForPropertyRestrictions() {
                    return useForPropertyRestrictions;
                }

                @Override
                public boolean useForPrimaryTypes() {
                    return useForPrimaryTypes;
                }

                @Override
                public boolean useForPathRestrictions() {
                    return useForPathRestrictions;
                }

                @Override
                public Collection<String> getIgnoredProperties() {
                    return Arrays.asList(ignoredProperties);
                }
            };
        }
        return oakSolrConfiguration;
    }
}
