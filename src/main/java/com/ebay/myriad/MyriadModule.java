/**
 * Copyright 2012-2014 eBay Software Foundation, All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.ebay.myriad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.myriad.configuration.MyriadConfiguration;
import com.ebay.myriad.scheduler.MyriadDriver;
import com.ebay.myriad.scheduler.MyriadDriverManager;
import com.ebay.myriad.scheduler.MyriadScheduler;
import com.ebay.myriad.scheduler.NMProfileManager;
import com.ebay.myriad.state.SchedulerState;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class MyriadModule extends AbstractModule {
	private final static Logger LOGGER = LoggerFactory
			.getLogger(MyriadModule.class);

	private MyriadConfiguration cfg;

	public MyriadModule(MyriadConfiguration cfg) {
		this.cfg = cfg;
	}

	@Override
	protected void configure() {
		LOGGER.debug("Configuring guice");
		bind(MyriadConfiguration.class).toInstance(cfg);
		bind(MyriadDriver.class).in(Scopes.SINGLETON);
		bind(MyriadDriverManager.class).in(Scopes.SINGLETON);
		bind(MyriadScheduler.class).in(Scopes.SINGLETON);
		bind(NMProfileManager.class).in(Scopes.SINGLETON);
		bind(SchedulerState.class).in(Scopes.SINGLETON);
	}
}
