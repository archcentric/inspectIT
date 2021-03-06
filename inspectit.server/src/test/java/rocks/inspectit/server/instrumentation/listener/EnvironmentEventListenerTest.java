package rocks.inspectit.server.instrumentation.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Sets;

import rocks.inspectit.server.ci.event.AgentMappingsUpdateEvent;
import rocks.inspectit.server.ci.event.EnvironmentUpdateEvent;
import rocks.inspectit.server.instrumentation.NextGenInstrumentationManager;
import rocks.inspectit.server.instrumentation.config.AgentCacheEntry;
import rocks.inspectit.server.instrumentation.config.ConfigurationHolder;
import rocks.inspectit.server.instrumentation.config.job.EnvironmentUpdateJob;
import rocks.inspectit.shared.all.testbase.TestBase;
import rocks.inspectit.shared.cs.ci.Environment;

/**
 * Tests the {@link EnvironmentEventListener} class.
 *
 * @author Marius Oehler
 *
 */
@SuppressWarnings({ "PMD" })
public class EnvironmentEventListenerTest extends TestBase {

	@InjectMocks
	EnvironmentEventListener eventListener;

	@Mock
	Logger log;

	@Mock
	NextGenInstrumentationManager nextGenInstrumentationManager;

	@Mock
	ExecutorService executor;

	@Mock
	ObjectFactory<EnvironmentUpdateJob> objectFactory;

	/**
	 * Tests the {@link EnvironmentEventListener#onApplicationEvent(AgentMappingsUpdateEvent)}
	 * method.
	 */
	public static class OnApplicationEvent extends EnvironmentEventListenerTest {

		@Mock
		EnvironmentUpdateEvent event;

		@Mock
		AgentCacheEntry cacheEntry;

		@Mock
		ConfigurationHolder configurationHolder;

		@Mock
		Environment environment;

		@Mock
		EnvironmentUpdateJob updateJob;

		@Mock
		Future<?> future;

		Map<Long, AgentCacheEntry> cacheMap;

		@BeforeMethod
		public void beforeMethod() {
			cacheMap = new HashMap<>();
			when(nextGenInstrumentationManager.getAgentCacheMap()).thenReturn(cacheMap);
			when(cacheEntry.getConfigurationHolder()).thenReturn(configurationHolder);
			when(configurationHolder.getEnvironment()).thenReturn(environment);
		}

		@Test
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void successful() throws InterruptedException, ExecutionException, TimeoutException {
			cacheMap.put(1L, cacheEntry);
			when(configurationHolder.isInitialized()).thenReturn(true);
			when(environment.getId()).thenReturn("id");
			when(event.getEnvironmentId()).thenReturn("id");
			when(objectFactory.getObject()).thenReturn(updateJob);
			when(executor.submit(updateJob)).thenReturn((Future) future);

			eventListener.onApplicationEvent(event);

			verify(nextGenInstrumentationManager).getAgentCacheMap();
			verify(cacheEntry).getConfigurationHolder();
			verify(configurationHolder).isInitialized();
			verify(configurationHolder).getEnvironment();
			verify(environment).getId();
			verify(event).getEnvironmentId();
			verify(objectFactory).getObject();
			verify(updateJob).setAgentCacheEntry(cacheEntry);
			verify(updateJob).setEnvironmentUpdateEvent(event);
			verify(executor).submit(updateJob);
			verify(future).get(1, TimeUnit.MINUTES);
			verifyNoMoreInteractions(nextGenInstrumentationManager, cacheEntry, configurationHolder, environment, event, objectFactory, updateJob, executor, future);
		}

		@Test
		public void emptyCacheMap() {
			eventListener.onApplicationEvent(event);

			verify(nextGenInstrumentationManager).getAgentCacheMap();
			verifyNoMoreInteractions(nextGenInstrumentationManager);
			verifyZeroInteractions(nextGenInstrumentationManager, cacheEntry, configurationHolder, environment, event, objectFactory, updateJob, executor, future);
		}

		@Test
		public void configurationNotInitialized() {
			cacheMap.put(1L, cacheEntry);
			when(configurationHolder.isInitialized()).thenReturn(false);

			eventListener.onApplicationEvent(event);

			verify(nextGenInstrumentationManager).getAgentCacheMap();
			verify(cacheEntry).getConfigurationHolder();
			verify(configurationHolder).isInitialized();
			verifyNoMoreInteractions(nextGenInstrumentationManager, cacheEntry, configurationHolder);
			verifyZeroInteractions(environment, event, objectFactory, updateJob, executor, future);
		}

		@Test
		public void environmentsAreNotEqual() {
			cacheMap.put(1L, cacheEntry);
			when(configurationHolder.isInitialized()).thenReturn(true);
			when(environment.getId()).thenReturn("id");
			when(event.getEnvironmentId()).thenReturn("otherId");

			eventListener.onApplicationEvent(event);

			verify(nextGenInstrumentationManager).getAgentCacheMap();
			verify(cacheEntry).getConfigurationHolder();
			verify(configurationHolder).isInitialized();
			verify(configurationHolder).getEnvironment();
			verify(environment).getId();
			verify(event).getEnvironmentId();
			verifyNoMoreInteractions(nextGenInstrumentationManager, cacheEntry, configurationHolder, environment, event);
			verifyZeroInteractions(objectFactory, updateJob, executor, future);
		}

		@Test
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void futureThrowsExecutionException() throws InterruptedException, ExecutionException, TimeoutException {
			cacheMap.put(1L, cacheEntry);
			when(configurationHolder.isInitialized()).thenReturn(true);
			when(environment.getProfileIds()).thenReturn(Sets.newHashSet("id_1"));
			when(objectFactory.getObject()).thenReturn(updateJob);
			when(executor.submit(updateJob)).thenReturn((Future) future);
			when(future.get(1L, TimeUnit.MINUTES)).thenThrow(ExecutionException.class);

			eventListener.onApplicationEvent(event);

			verify(future).get(1, TimeUnit.MINUTES);
		}

		@Test
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void futureThrowsInterruptedException() throws InterruptedException, ExecutionException, TimeoutException {
			cacheMap.put(1L, cacheEntry);
			when(configurationHolder.isInitialized()).thenReturn(true);
			when(environment.getProfileIds()).thenReturn(Sets.newHashSet("id_1"));
			when(objectFactory.getObject()).thenReturn(updateJob);
			when(executor.submit(updateJob)).thenReturn((Future) future);
			when(future.get(1L, TimeUnit.MINUTES)).thenThrow(InterruptedException.class);

			eventListener.onApplicationEvent(event);

			verify(future).get(1, TimeUnit.MINUTES);
		}
	}
}
