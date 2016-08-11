package avalanche.core.persistence;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import avalanche.core.AndroidTestUtils;
import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.json.DefaultLogSerializer;
import avalanche.core.ingestion.models.json.LogSerializer;
import avalanche.core.ingestion.models.json.MockLogFactory;
import avalanche.core.persistence.AvalanchePersistence.PersistenceException;
import avalanche.core.utils.StorageHelper;
import avalanche.core.utils.StorageHelper.DatabaseStorage.DatabaseScanner;

import static avalanche.core.ingestion.models.json.MockLog.MOCK_LOG_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@SuppressWarnings("unused")
@MediumTest
@RunWith(AndroidJUnit4.class)
public class AvalancheDatabasePersistenceTest {

    /**
     * Log tag.
     */
    private static final String TAG = "DatabasePersistenceTest";

    /**
     * Context instance.
     */
    private static Context sContext;

    @BeforeClass
    public static void setUpClass() {
        sContext = InstrumentationRegistry.getTargetContext();
        StorageHelper.initialize(sContext);

        /* Clean up database. */
        sContext.deleteDatabase("test-persistence");
    }

    @AfterClass
    public static void tearDownClass() {
        /* Clean up database. */
        sContext.deleteDatabase("test-persistence");
    }

    private static int getIteratorSize(Iterator iterator) {
        int count = 0;
        for (; iterator.hasNext(); iterator.next())
            count++;
        return count;
    }

    @Test
    public void putLog() throws PersistenceException, IOException {
        android.util.Log.i(TAG, "Testing Database Persistence putLog");

        /* Initialize database persistence. */
        AvalancheDatabasePersistence persistence = new AvalancheDatabasePersistence("test-persistence", "putLog", 1);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);

        try {
            /* Generate a log and persist. */
            Log log = AndroidTestUtils.generateMockLog();
            persistence.putLog("test-p1", log);

            /* Get a log from persistence. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test-p1", 1, outputLogs);
            assertEquals(1, outputLogs.size());
            assertEquals(log, outputLogs.get(0));
        } finally {
            /* Close. */
            persistence.close();
        }
    }

    @Test
    public void putTooManyLogs() throws PersistenceException, IOException {
        android.util.Log.i(TAG, "Testing Database Persistence putLog with too many logs");

        /* Initialize database persistence. */
        AvalancheDatabasePersistence persistence = new AvalancheDatabasePersistence("test-persistence", "putTooManyLogs", 1, 2);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);

        try {
            /* Generate too many logs and persist. */
            Log log1 = AndroidTestUtils.generateMockLog();
            Log log2 = AndroidTestUtils.generateMockLog();
            Log log3 = AndroidTestUtils.generateMockLog();
            Log log4 = AndroidTestUtils.generateMockLog();
            persistence.putLog("test-p1", log1);
            persistence.putLog("test-p1", log2);
            persistence.putLog("test-p1", log3);
            persistence.putLog("test-p1", log4);

            /* Get logs from persistence. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test-p1", 4, outputLogs);
            assertEquals(2, outputLogs.size());
            assertEquals(log3, outputLogs.get(0));
            assertEquals(log4, outputLogs.get(1));
        } finally {
            /* Close. */
            persistence.close();
        }
    }

    @Test(expected = PersistenceException.class)
    public void putLogException() throws PersistenceException, IOException, JSONException {
        android.util.Log.i(TAG, "Testing Database Persistence putLog with serialize exception");

        /* Initialize database persistence. */
        AvalancheDatabasePersistence persistence = new AvalancheDatabasePersistence("test-persistence", "putLogException", 1);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = mock(LogSerializer.class);
        doThrow(new JSONException("JSON exception")).when(logSerializer).serializeLog(any(Log.class));
        persistence.setLogSerializer(logSerializer);

        try {
            /* Generate a log and persist. */
            Log log = AndroidTestUtils.generateMockLog();
            persistence.putLog("test-p1", log);
        } finally {
            /* Close. */
            persistence.close();
        }
    }

    @Test
    public void deleteLogs() throws PersistenceException, IOException {
        android.util.Log.i(TAG, "Testing Database Persistence deleteLogs");

        /* Initialize database persistence. */
        AvalancheDatabasePersistence persistence = new AvalancheDatabasePersistence("test-persistence", "deleteLogs", 1);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);

        try {
            /* Generate a log and persist. */
            Log log1 = AndroidTestUtils.generateMockLog();
            Log log2 = AndroidTestUtils.generateMockLog();
            Log log3 = AndroidTestUtils.generateMockLog();
            Log log4 = AndroidTestUtils.generateMockLog();
            persistence.putLog("test-p1", log1);
            persistence.putLog("test-p1", log2);
            persistence.putLog("test-p2", log3);
            persistence.putLog("test-p3", log4);

            /* Get a log from persistence. */
            List<Log> outputLogs1 = new ArrayList<>();
            List<Log> outputLogs2 = new ArrayList<>();
            List<Log> outputLogs3 = new ArrayList<>();
            String id = persistence.getLogs("test-p1", 5, outputLogs1);
            persistence.getLogs("test-p2", 5, outputLogs2);
            persistence.getLogs("test-p3", 5, outputLogs3);

            /* Verify. */
            assertNotNull(id);
            assertNotEquals("", id);
            assertEquals(2, outputLogs1.size());
            assertEquals(1, outputLogs2.size());
            assertEquals(1, outputLogs3.size());

            /* Delete. */
            persistence.deleteLogs("", id);

            /* Access DatabaseStorage directly to verify the deletions. */
            DatabaseScanner scanner1 = persistence.mDatabaseStorage.getScanner(AvalancheDatabasePersistence.COLUMN_GROUP, "test-p1");
            DatabaseScanner scanner2 = persistence.mDatabaseStorage.getScanner(AvalancheDatabasePersistence.COLUMN_GROUP, "test-p2");
            DatabaseScanner scanner3 = persistence.mDatabaseStorage.getScanner(AvalancheDatabasePersistence.COLUMN_GROUP, "test-p3");

            //noinspection TryFinallyCanBeTryWithResources
            try {
                /* Verify. */
                assertEquals(2, getIteratorSize(scanner1.iterator()));
                assertEquals(1, getIteratorSize(scanner2.iterator()));
                assertEquals(1, getIteratorSize(scanner3.iterator()));
            } finally {
                /* Close. */
                scanner1.close();
                scanner2.close();
                scanner3.close();
            }

            /* Delete. */
            persistence.deleteLogs("test-p1", id);

            /* Access DatabaseStorage directly to verify the deletions. */
            DatabaseScanner scanner4 = persistence.mDatabaseStorage.getScanner(AvalancheDatabasePersistence.COLUMN_GROUP, "test-p1");

            //noinspection TryFinallyCanBeTryWithResources
            try {
                /* Verify. */
                assertEquals(0, getIteratorSize(scanner4.iterator()));
            } finally {
                /* Close. */
                scanner4.close();
            }

        } finally {
            /* Close. */
            persistence.close();
        }
    }

    @Test
    public void deleteLogsForGroup() throws PersistenceException, IOException {
        android.util.Log.i(TAG, "Testing Database Persistence deleteLogs for group");

        /* Initialize database persistence. */
        AvalancheDatabasePersistence persistence = new AvalancheDatabasePersistence("test-persistence", "deleteLogsForGroup", 1);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);

        try {
            /* Generate a log and persist. */
            Log log1 = AndroidTestUtils.generateMockLog();
            Log log2 = AndroidTestUtils.generateMockLog();
            Log log3 = AndroidTestUtils.generateMockLog();
            Log log4 = AndroidTestUtils.generateMockLog();
            persistence.putLog("test-p1", log1);
            persistence.putLog("test-p1", log2);
            persistence.putLog("test-p2", log3);
            persistence.putLog("test-p3", log4);

            /* Get a log from persistence. */
            List<Log> outputLogs = new ArrayList<>();
            String id1 = persistence.getLogs("test-p1", 5, outputLogs);
            String id2 = persistence.getLogs("test-p2", 5, outputLogs);
            assertNotNull(id1);
            assertNotNull(id2);

            /* Delete. */
            persistence.deleteLogs("test-p1");
            persistence.deleteLogs("test-p3");

            /* Try another get for verification. */
            outputLogs.clear();
            persistence.getLogs("test-p3", 5, outputLogs);

            /* Verify. */
            Map<String, List<Long>> pendingGroups = persistence.getPendingDbIdentifiersGroups();
            assertNull(pendingGroups.get("test-p1" + id1));
            assertEquals(1, pendingGroups.get("test-p2" + id2).size());
            assertEquals(1, pendingGroups.size());
            assertEquals(0, outputLogs.size());
            assertEquals(1, persistence.mDatabaseStorage.size());

            /* Verify one log still persists in the database. */
            persistence.clearPendingLogState();
            outputLogs.clear();
            persistence.getLogs("test-p2", 5, outputLogs);
            assertEquals(1, outputLogs.size());
            assertEquals(log3, outputLogs.get(0));
        } finally {
            /* Close. */
            persistence.close();
        }
    }

    @Test
    public void getLogs() throws PersistenceException, IOException {
        android.util.Log.i(TAG, "Testing Database Persistence getLogs");

        /* Initialize database persistence. */
        AvalancheDatabasePersistence persistence = new AvalancheDatabasePersistence("test-persistence", "getLogs", 1);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = new DefaultLogSerializer();
        logSerializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        persistence.setLogSerializer(logSerializer);

        try {
            /* Test constants. */
            int numberOfLogs = 10;
            int sizeForGetLogs = 4;

            /* Generate a log and persist. */
            Log[] logs = new Log[numberOfLogs];
            for (int i = 0; i < logs.length; i++)
                logs[i] = AndroidTestUtils.generateMockLog();

            /* Put. */
            for (Log log : logs)
                persistence.putLog("test", log);

            /* Get. */
            getAllLogs(persistence, "test", numberOfLogs, sizeForGetLogs);

            /* Clear ids, we should be able to get the logs again in the same sequence. */
            persistence.clearPendingLogState();
            getAllLogs(persistence, "test", numberOfLogs, sizeForGetLogs);

            /* Clear. Nothing to get after. */
            persistence.clear();
            List<Log> outputLogs = new ArrayList<>();
            assertNull(persistence.getLogs("test", sizeForGetLogs, outputLogs));
            assertTrue(outputLogs.isEmpty());
        } finally {
            /* Close. */
            persistence.close();
        }
    }

    private void getAllLogs(AvalancheDatabasePersistence persistence, String group, int numberOfLogs, int sizeForGetLogs) {
        List<Log> outputLogs = new ArrayList<>();
        int expected = 0;
        do {
            numberOfLogs -= expected;
            persistence.getLogs(group, sizeForGetLogs, outputLogs);
            expected = Math.min(Math.max(numberOfLogs, 0), sizeForGetLogs);
            assertEquals(expected, outputLogs.size());
            outputLogs.clear();
        } while (numberOfLogs > 0);

        /* Get should be 0 now. */
        persistence.getLogs(group, sizeForGetLogs, outputLogs);
        assertEquals(0, outputLogs.size());
    }

    @Test
    public void getLogsException() throws PersistenceException, IOException, JSONException {
        android.util.Log.i(TAG, "Testing Database Persistence getLogs with deserialize exception");

        /* Initialize database persistence. */
        AvalancheDatabasePersistence persistence = new AvalancheDatabasePersistence("test-persistence", "getLogs", 1);

        /* Set a mock log serializer. */
        LogSerializer logSerializer = spy(new DefaultLogSerializer());
        /* Throw a JSON exception for the first call. */
        doThrow(new JSONException("JSON exception"))
                /* Return a normal log for the second call. */
                .doReturn(AndroidTestUtils.generateMockLog())
                /* Throw a JSON exception for the third call. */
                .doThrow(new JSONException("JSON exception"))
                /* Return a normal log for further calls. */
                .doReturn(AndroidTestUtils.generateMockLog())
                .when(logSerializer).deserializeLog(anyString());
        persistence.setLogSerializer(logSerializer);

        try {
            /* Test constants. */
            int numberOfLogs = 4;

            /* Generate a log and persist. */
            Log[] logs = new Log[numberOfLogs];
            for (int i = 0; i < logs.length; i++)
                logs[i] = AndroidTestUtils.generateMockLog();

            /* Put. */
            for (Log log : logs)
                persistence.putLog("test", log);

            /* Get. */
            List<Log> outputLogs = new ArrayList<>();
            persistence.getLogs("test", 10, outputLogs);
            assertEquals(numberOfLogs / 2, outputLogs.size());
            assertEquals(2, persistence.mDatabaseStorage.size());
        } finally {
            /* Close. */
            persistence.close();
        }
    }
}