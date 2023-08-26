/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslParsingStory.DSL_PARSING_STORY;

import org.mule.functional.api.component.SharedConfig;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import java.util.Collection;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(MULE_DSL)
@Story(DSL_PARSING_STORY)
public class XmlDslRegistryLookupByAbstractType extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/parsers/dsl-registry-lookup-by-abstract-type.xml";
  }

  @Test
  @Description("Tests that registry lookups work as expected for objects created from the Mule config through a DSL parser.")
  public void configObjectsLookup() {
    // Lookup by concrete class
    Collection<SharedConfig> sharedConfigs = registry.lookupAllByType(SharedConfig.class);
    assertThat(sharedConfigs, hasSize(1));

    SharedConfig sharedConfig = sharedConfigs.iterator().next();

    // Lookup by superclass/interfaces
    assertThat(registry.lookupAllByType(AbstractComponent.class), hasItem(sharedConfig));
    assertThat(registry.lookupAllByType(Initialisable.class), hasItem(sharedConfig));
  }
}
