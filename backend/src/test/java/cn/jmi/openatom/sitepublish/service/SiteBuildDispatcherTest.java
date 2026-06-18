package cn.jmi.openatom.sitepublish.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Path;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SiteBuildDispatcherTest {

    private final SiteBuildWorker buildWorker = mock(SiteBuildWorker.class);
    private final SiteBuildDispatcher dispatcher = new SiteBuildDispatcher(buildWorker);

    @TempDir
    Path sourceDirectory;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void waitsForTransactionCommitBeforeStartingAsyncBuild() {
        TransactionSynchronizationManager.initSynchronization();

        dispatcher.dispatchAfterCommit(101L, 202L, sourceDirectory);

        verifyNoInteractions(buildWorker);
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);

        verify(buildWorker).build(101L, 202L, sourceDirectory.toAbsolutePath());
    }

    @Test
    void startsImmediatelyWhenThereIsNoTransaction() {
        dispatcher.dispatchAfterCommit(303L, 404L, sourceDirectory);

        verify(buildWorker).build(303L, 404L, sourceDirectory.toAbsolutePath());
    }
}
