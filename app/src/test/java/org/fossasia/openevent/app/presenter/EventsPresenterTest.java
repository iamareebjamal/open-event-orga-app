package org.fossasia.openevent.app.presenter;

import org.fossasia.openevent.app.common.ContextManager;
import org.fossasia.openevent.app.data.repository.contract.IEventRepository;
import org.fossasia.openevent.app.data.contract.ILoginModel;
import org.fossasia.openevent.app.data.models.Event;
import org.fossasia.openevent.app.data.models.User;
import org.fossasia.openevent.app.events.EventsPresenter;
import org.fossasia.openevent.app.events.contract.IEventsView;
import org.fossasia.openevent.app.utils.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

import static org.fossasia.openevent.app.presenter.Util.ERROR_OBSERVABLE;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class EventsPresenterTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    IEventsView eventListView;

    @Mock
    IEventRepository eventRepository;

    @Mock
    ILoginModel loginModel;

    @Mock
    ContextManager contextManager;

    private EventsPresenter eventsActivityPresenter;

    private String date = DateUtils.formatDateToIso(new Date());

    private List<Event> eventList = Arrays.asList(
        new Event(12, date, date),
        new Event(13, date, date),
        new Event(14, date, date)
    );

    private User organiser = new User();

    @Before
    public void setUp() {
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> Schedulers.trampoline());

        eventsActivityPresenter = new EventsPresenter(eventRepository, contextManager);
        eventsActivityPresenter.attach(eventListView);
    }

    @After
    public void tearDown() {
        RxJavaPlugins.reset();
    }

    @Test
    public void shouldLoadEventsAndOrganiserAutomatically() {
        when(eventRepository.getOrganiser(false))
            .thenReturn(Observable.just(organiser));

        when(eventRepository.getEvents(false))
            .thenReturn(Observable.fromIterable(eventList));

        eventsActivityPresenter.start();

        verify(eventRepository).getEvents(false);
        verify(eventRepository).getOrganiser(false);
    }

    @Test
    public void shouldDetachViewOnStop() {
        assertNotNull(eventsActivityPresenter.getView());

        eventsActivityPresenter.detach();

        assertNull(eventsActivityPresenter.getView());
    }

    @Test
    public void shouldLoadEventsSuccessfully() {
        when(eventRepository.getEvents(false))
            .thenReturn(Observable.fromIterable(eventList));

        InOrder inOrder = Mockito.inOrder(eventRepository, eventListView);

        eventsActivityPresenter.loadUserEvents(false);

        inOrder.verify(eventRepository).getEvents(false);
        inOrder.verify(eventListView).showProgress(true);
        inOrder.verify(eventListView).showResults(eventList);
        inOrder.verify(eventListView).showProgress(false);
    }

    @Test
    public void shouldRefreshEventsSuccessfully() {
        when(eventRepository.getEvents(true))
            .thenReturn(Observable.fromIterable(eventList));

        InOrder inOrder = Mockito.inOrder(eventRepository, eventListView);

        eventsActivityPresenter.loadUserEvents(true);

        inOrder.verify(eventRepository).getEvents(true);
        inOrder.verify(eventListView).showProgress(true);
        inOrder.verify(eventListView).showResults(eventList);
        inOrder.verify(eventListView).onRefreshComplete();
        inOrder.verify(eventListView).showProgress(false);
    }

    @Test
    public void shouldShowEmptyViewOnNoItemAfterSwipeRefresh() {
        ArrayList<Event> events = new ArrayList<>();
        when(eventRepository.getEvents(true))
            .thenReturn(Observable.fromIterable(events));

        InOrder inOrder = Mockito.inOrder(eventListView);

        eventsActivityPresenter.loadUserEvents(true);

        inOrder.verify(eventListView).showEmptyView(false);
        inOrder.verify(eventListView).showResults(events);
        inOrder.verify(eventListView).showEmptyView(true);
    }

    @Test
    public void shouldShowEmptyViewOnSwipeRefreshError() {
        when(eventRepository.getEvents(true))
            .thenReturn(ERROR_OBSERVABLE);

        InOrder inOrder = Mockito.inOrder(eventListView);

        eventsActivityPresenter.loadUserEvents(true);

        inOrder.verify(eventListView).showEmptyView(false);
        inOrder.verify(eventListView).showError(anyString());
        inOrder.verify(eventListView).showEmptyView(true);
    }

    @Test
    public void shouldNotShowEmptyViewOnSwipeRefreshSuccess() {
        when(eventRepository.getEvents(true))
            .thenReturn(Observable.fromIterable(eventList));

        InOrder inOrder = Mockito.inOrder(eventListView);

        eventsActivityPresenter.loadUserEvents(true);

        inOrder.verify(eventListView).showEmptyView(false);
        inOrder.verify(eventListView).showResults(eventList);
        inOrder.verify(eventListView).showEmptyView(false);
    }

    @Test
    public void shouldRefreshEventsOnError() {
        when(eventRepository.getEvents(true))
            .thenReturn(ERROR_OBSERVABLE);

        InOrder inOrder = Mockito.inOrder(eventRepository, eventListView);

        eventsActivityPresenter.loadUserEvents(true);

        inOrder.verify(eventRepository).getEvents(true);
        inOrder.verify(eventListView).showProgress(true);
        inOrder.verify(eventListView).showError(anyString());
        inOrder.verify(eventListView).onRefreshComplete();
        inOrder.verify(eventListView).showProgress(false);
    }

    @Test
    public void shouldShowEventError() {
        String error = "Test Error";
        when(eventRepository.getEvents(false))
            .thenReturn(ERROR_OBSERVABLE);

        InOrder inOrder = Mockito.inOrder(eventRepository, eventListView);

        eventsActivityPresenter.loadUserEvents(false);

        inOrder.verify(eventRepository).getEvents(false);
        inOrder.verify(eventListView).showProgress(true);
        inOrder.verify(eventListView).showError(error);
        inOrder.verify(eventListView).showProgress(false);
    }

    @Test
    public void shouldLoadOrganiserSuccessfully() {
        organiser.setFirstName("John");
        organiser.setLastName("Wick");

        when(eventRepository.getOrganiser(false)).thenReturn(Observable.just(organiser));

        InOrder inOrder = Mockito.inOrder(eventRepository, eventListView);

        eventsActivityPresenter.loadOrganiser(false);

        inOrder.verify(eventRepository).getOrganiser(false);
        inOrder.verify(eventListView).showOrganiserName("John Wick");
    }

    @Test
    public void shouldSetSentryContext() {
        when(eventRepository.getOrganiser(false)).thenReturn(Observable.just(organiser));

        eventsActivityPresenter.loadOrganiser(false);

        verify(contextManager).setOrganiser(organiser);
    }

    @Test
    public void shouldShowOrganiserError() {
        String error = "Test Error";
        when(eventRepository.getOrganiser(false))
            .thenReturn(ERROR_OBSERVABLE);

        InOrder inOrder = Mockito.inOrder(eventRepository, eventListView);

        eventsActivityPresenter.loadOrganiser(false);

        inOrder.verify(eventRepository).getOrganiser(false);
        inOrder.verify(eventListView).showOrganiserLoadError(error);
    }

    @Test
    public void shouldNotAccessView() {
        eventsActivityPresenter.detach();

        eventsActivityPresenter.loadUserEvents(false);
        eventsActivityPresenter.loadOrganiser(false);

        verifyNoMoreInteractions(eventListView);
    }

}
