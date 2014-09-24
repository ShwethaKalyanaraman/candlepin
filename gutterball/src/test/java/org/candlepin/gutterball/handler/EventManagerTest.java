/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.gutterball.handler;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.candlepin.gutterball.curator.jpa.ConsumerStateCurator;
import org.candlepin.gutterball.curator.jpa.EventCurator;
import org.candlepin.gutterball.eventhandler.ConsumerHandler;
import org.candlepin.gutterball.eventhandler.EventHandler;
import org.candlepin.gutterball.eventhandler.EventManager;
import org.candlepin.gutterball.eventhandler.HandlerTarget;
import org.candlepin.gutterball.model.jpa.ConsumerState;
import org.candlepin.gutterball.model.jpa.Event;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class EventManagerTest {

    // TODO: might need to expand this in the future
    public static final String CONSUMER_JSON = "{\"id\": \"someId\"}";

    @Mock
    private ConsumerStateCurator consumerStateCurator;

    @Mock
    private EventCurator eventCurator;

    private ConsumerHandler consumerHandler;

    private EventManager eventManager;

    @Before
    public void before() {
        consumerHandler = new ConsumerHandler(consumerStateCurator);
        Map<String, EventHandler> handlers = new HashMap<String, EventHandler>();
        handlers.put(ConsumerHandler.class.getAnnotation(HandlerTarget.class).value(), consumerHandler);
        eventManager = new TestingEventManager(handlers, eventCurator);
    }

    @Test
    public void testEventManagerUnknown() {
        Event toHandle = new Event();
        toHandle.setTarget("UNKNOWN_EVENT_TARGET");
        eventManager.handle(toHandle);
        verify(consumerStateCurator, never()).create(any(ConsumerState.class));
        verify(eventCurator).create(eq(toHandle));
    }

    @Test
    public void testEventManagerNullTarget() {
        Event toHandle = new Event();
        eventManager.handle(toHandle);
        verify(consumerStateCurator, never()).create(any(ConsumerState.class));
        verify(eventCurator).create(eq(toHandle));
    }

    // Class allows us to override loadEventHandlers, so we can supply mocks
    // We aren't testing the DB, so we want to avoid the curators
    private class TestingEventManager extends EventManager {

        public TestingEventManager(Map<String, EventHandler> handlers, EventCurator curator) {
            super(handlers, curator);
        }
    }
}
