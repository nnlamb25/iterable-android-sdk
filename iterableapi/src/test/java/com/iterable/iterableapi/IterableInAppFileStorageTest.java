package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(TestRunner.class)
public class IterableInAppFileStorageTest {

    @Test
    public void testInAppPersistence() throws Exception {
        IterableInAppFileStorage storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        IterableInAppMessage testInAppMessage = InAppTestUtils.getTestInAppMessage();

        assertEquals(0, storage.getMessages().size());

        storage.addMessage(testInAppMessage);
        assertEquals(1, storage.getMessages().size());
        assertEquals(storage.getMessage(testInAppMessage.getMessageId()), testInAppMessage);
        testInAppMessage.setProcessed(true);
        testInAppMessage.setConsumed(true);
        storage.save();

        storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        assertEquals(1, storage.getMessages().size());
        JSONAssert.assertEquals(testInAppMessage.toJSONObject(), storage.getMessages().get(0).toJSONObject(), JSONCompareMode.STRICT);
        assertTrue(storage.getMessages().get(0).isProcessed());
        assertTrue(storage.getMessages().get(0).isConsumed());

        storage.removeMessage(storage.getMessage(testInAppMessage.getMessageId()));
        assertEquals(0, storage.getMessages().size());
        storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        assertEquals(0, storage.getMessages().size());
    }
}
