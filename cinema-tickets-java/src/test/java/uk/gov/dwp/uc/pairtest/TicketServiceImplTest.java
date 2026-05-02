package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final long VALID_ACCOUNT_ID = 1L;

    @Mock
    private TicketPaymentService ticketPaymentService;

    @Mock
    private SeatReservationService seatReservationService;

    @Captor
    ArgumentCaptor<Integer> integerArgumentCaptor;

    @Captor
    ArgumentCaptor<Long> longArgumentCaptor;

    TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(ticketPaymentService, seatReservationService);
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Nested
        @DisplayName("Request Validation")
        class RequestValidationTests {
            @Test
            @DisplayName("Reject if accountId is null")
            void shouldThrowInvalidPurchaseExceptionIfAccountIdIsNull() {

                TicketTypeRequest[] ticketTypeRequests = {
                    new TicketTypeRequest(Type.ADULT, 15),
                    new TicketTypeRequest(Type.CHILD, 15),
                    new TicketTypeRequest(Type.INFANT, 15)
                };

                InvalidPurchaseException exception = assertThrows(
                    InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(null, ticketTypeRequests)
                );

                assertEquals("You must provide a valid account Id", exception.getMessage());
                verifyNoInteractions(ticketPaymentService, seatReservationService);
            }

            @ParameterizedTest
            @ValueSource(ints = {0, -1, -10, -15, -23, -25, -60})
            @DisplayName("Reject if accountId is less than 1")
            void shouldThrowInvalidPurchaseExceptionIfAccountIdIsLessThan1() {

                TicketTypeRequest[] ticketTypeRequests = {
                    new TicketTypeRequest(Type.ADULT, 15),
                    new TicketTypeRequest(Type.CHILD, 15),
                    new TicketTypeRequest(Type.INFANT, 15)
                };

                InvalidPurchaseException exception = assertThrows(
                    InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(null, ticketTypeRequests)
                );

                assertEquals("You must provide a valid account Id", exception.getMessage());
                verifyNoInteractions(ticketPaymentService, seatReservationService);
            }

            @ParameterizedTest
            @NullAndEmptySource
            @DisplayName("Reject if TicketTypeRequests is null or empty")
            void shouldThrowInvalidPurchaseExceptionIfTicketType(TicketTypeRequest[] ticketTypeRequests) {

                InvalidPurchaseException exception = assertThrows(
                    InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests)
                );

                assertEquals("You must request at least 1 ticket", exception.getMessage());
                verifyNoInteractions(ticketPaymentService, seatReservationService);
            }

            @Test
            @DisplayName("Reject requests with a of total 0 tickets")
            void shouldThrowInvalidPurchaseExceptionIfTotalTicketsEqualZero() {

                TicketTypeRequest[] ticketTypeRequests = {
                    new TicketTypeRequest(Type.ADULT, 0),
                    new TicketTypeRequest(Type.CHILD, 0),
                    new TicketTypeRequest(Type.INFANT, 0)
                };

                InvalidPurchaseException exception = assertThrows(
                    InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests)
                );

                assertEquals("You must request at least 1 ticket", exception.getMessage());
                verifyNoInteractions(ticketPaymentService, seatReservationService);
            }

            @Test
            @DisplayName("Reject requests if there is a null ticket request")
            void shouldThrowInvalidPurchaseExceptionIfThereIsANullTicketRequest() {

                TicketTypeRequest[] ticketTypeRequests = {
                    new TicketTypeRequest(Type.ADULT, 5),
                    null,
                    new TicketTypeRequest(Type.INFANT, 5)
                };

                InvalidPurchaseException exception = assertThrows(
                    InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests)
                );

                assertEquals("Ticket request must not be null", exception.getMessage());
                verifyNoInteractions(ticketPaymentService, seatReservationService);
            }

        }

        @Nested
        @DisplayName("Business Rules Validation")
        class BusinessRulesValidationTests {

            @Test
            @DisplayName("Reject if no ADULT tickets are requested with CHILD tickets")
            void shouldThrowInvalidPurchaseExceptionIfChildTicketsAreRequestedWithNoAdults() {

                TicketTypeRequest[] ticketTypeRequests = {
                    new TicketTypeRequest(Type.CHILD, 10)
                };

                InvalidPurchaseException exception = assertThrows(
                    InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests)
                );

                assertEquals("At least 1 ADULT ticket is required", exception.getMessage());
                verifyNoInteractions(ticketPaymentService, seatReservationService);
            }

            @ParameterizedTest
            @ValueSource(ints = {26, 36, 51, 356, 5757, 7979})
            @DisplayName("Reject if total requested tickets is greater than 25 from single request")
            void shouldThrowInvalidPurchaseExceptionWhenTotalRequestedTicketsIsGreaterThan25(int noOfTickets) {

                TicketTypeRequest ticketTypeRequest = new TicketTypeRequest(Type.ADULT, noOfTickets);

                InvalidPurchaseException exception = assertThrows(
                    InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequest)
                );

                assertEquals("You cannot request more than 25 tickets", exception.getMessage());
                verifyNoInteractions(ticketPaymentService, seatReservationService);
            }

            @Test
            @DisplayName("Reject if total requested tickets is greater than 25 from multiple requests")
            void shouldThrowInvalidPurchaseExceptionWhenTotalRequestedTicketsIsGreaterThan25() {

                TicketTypeRequest[] ticketTypeRequests = {
                    new TicketTypeRequest(Type.ADULT, 15),
                    new TicketTypeRequest(Type.CHILD, 15),
                    new TicketTypeRequest(Type.INFANT, 15)
                };

                InvalidPurchaseException exception = assertThrows(
                    InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests)
                );

                assertEquals("You cannot request more than 25 tickets", exception.getMessage());
                verifyNoInteractions(ticketPaymentService, seatReservationService);
            }

            @Test
            @DisplayName("Reject if total number of INFANT tickets is greater than ADULT tickets")
            void shouldThrowInvalidPurchaseExceptionIfInfantTicketsAreGreaterThanAdultTickets() {

                TicketTypeRequest[] ticketTypeRequests = {
                    new TicketTypeRequest(Type.ADULT, 5),
                    new TicketTypeRequest(Type.ADULT, 1),
                    new TicketTypeRequest(Type.INFANT, 5),
                    new TicketTypeRequest(Type.INFANT, 5)
                };

                InvalidPurchaseException exception = assertThrows(
                    InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests)
                );

                assertEquals("You cannot have more INFANTS than ADULTS", exception.getMessage());
                verifyNoInteractions(ticketPaymentService, seatReservationService);
            }
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Total amount to pay should be calculated correctly for multiple requests")
        void calculatesTheCorrectAmountToPayFromMultipleRequests() {
        /* total expect to pay is 325
        Adult 250
        Child  75
        Infant 0 */
            TicketTypeRequest[] ticketTypeRequests = {
                new TicketTypeRequest(Type.ADULT, 10),
                new TicketTypeRequest(Type.CHILD, 5),
                new TicketTypeRequest(Type.INFANT, 5)
            };

            ticketService.purchaseTickets(1L, ticketTypeRequests);

            verify(ticketPaymentService, times(1)).makePayment(anyLong(), integerArgumentCaptor.capture());

            assertEquals(325, integerArgumentCaptor.getValue());
        }

        @Test
        @DisplayName("Total amount to pay from a single request should be calculated correctly")
        void calculatesTheCorrectAmountToPayFromSingleRequest() {

            TicketTypeRequest ticketTypeRequest = new TicketTypeRequest(Type.ADULT, 10);

            ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequest);

            verify(ticketPaymentService, times(1)).makePayment(anyLong(), integerArgumentCaptor.capture());

            assertEquals(250, integerArgumentCaptor.getValue());
        }

        @Test
        @DisplayName("The correct number of seats should be reserved from a single request")
        void shouldReserveCorrectNumberOfSeatsFromASingleRequest() {

            TicketTypeRequest[] ticketTypeRequests = {
                new TicketTypeRequest(Type.ADULT, 10)
            };

            ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests);

            verify(seatReservationService, times(1)).reserveSeat(anyLong(), integerArgumentCaptor.capture());

            assertEquals(10, integerArgumentCaptor.getValue());
        }

        @Test
        @DisplayName("The correct number of seats should be reserved from multiple requests")
        void shouldReserveCorrectNumberOfSeatsFromMultipleRequests() {
        /* total seats required 15
        Adult 10
        Child  5
        Infant 0 */
            TicketTypeRequest[] ticketTypeRequests = {
                new TicketTypeRequest(Type.ADULT, 10),
                new TicketTypeRequest(Type.CHILD, 5),
                new TicketTypeRequest(Type.INFANT, 5)
            };

            ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests);

            verify(seatReservationService, times(1)).reserveSeat(anyLong(), integerArgumentCaptor.capture());

            assertEquals(15, integerArgumentCaptor.getValue());
        }

        @Test
        @DisplayName("Infant ticket requests should not reserve seats")
        void infantTicketsShouldNotReserveSeats() {

            int expectedSeats = 10;

            TicketTypeRequest[] ticketTypeRequests = {
                new TicketTypeRequest(Type.ADULT, expectedSeats),
                new TicketTypeRequest(Type.INFANT, 5)
            };

            ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests);

            verify(seatReservationService, times(1)).reserveSeat(anyLong(), integerArgumentCaptor.capture());

            assertEquals(expectedSeats, integerArgumentCaptor.getValue());
        }

        @Test
        @DisplayName("Infant tickets should not be charged")
        void mixedTicketRequestShouldChargeChildAndAdultButNotInfant() {
        /* total expect to pay is 40
        Adult 25
        Child  15
        Infant 0 */
            TicketTypeRequest[] ticketTypeRequests = {
                new TicketTypeRequest(Type.ADULT, 1),
                new TicketTypeRequest(Type.CHILD, 1),
                new TicketTypeRequest(Type.INFANT, 1)
            };

            ticketService.purchaseTickets(VALID_ACCOUNT_ID, ticketTypeRequests);

            verify(ticketPaymentService, times(1)).makePayment(anyLong(), integerArgumentCaptor.capture());

            assertEquals(40, integerArgumentCaptor.getValue());
        }
    }

    @Nested
    @DisplayName("Interaction Tests")
    class InteractionTests {

        @ParameterizedTest
        @ValueSource(longs = {1L, 25L, 36L, 57L, 79L, 80L, 123L, 3546456L, 5675868979L})
        @DisplayName("TicketPaymentService.makePayment should be called with correct account id")
        void makePaymentShouldBeCalledWithCorrectAccountId(long accountId) {

            ticketService.purchaseTickets(accountId, new TicketTypeRequest(Type.ADULT, 1));

            verify(ticketPaymentService, times(1)).makePayment(longArgumentCaptor.capture(), anyInt());

            assertEquals(accountId, longArgumentCaptor.getValue());
        }

        @ParameterizedTest
        @ValueSource(longs = {1L, 4L, 6L, 78L, 90L, 567L, 345L, 123L, 46575768L, 6789789L})
        @DisplayName("SeatReservationService.reserveSeat should only be called with correct account id")
        void reserveSeatsShouldBeCalledWithCorrectAccountId(long accountId) {

            ticketService.purchaseTickets(accountId, new TicketTypeRequest(Type.ADULT, 2));

            verify(seatReservationService, times(1)).reserveSeat(longArgumentCaptor.capture(), anyInt());

            assertEquals(accountId, longArgumentCaptor.getValue());
        }

        @Test
        @DisplayName("TicketPaymentService.makePayment should only be called once")
        void purchaseTicketsShouldCallTicketPaymentServiceOnlyOnce() {

            ticketService.purchaseTickets(VALID_ACCOUNT_ID, new TicketTypeRequest(Type.ADULT, 2));

            verify(ticketPaymentService, times(1)).makePayment(anyLong(), anyInt());
        }

        @Test
        @DisplayName("SeatReservationService.reserveSeat should only be called once")
        void purchaseTicketsShouldCallReserveSeatsOnlyOnce() {

            ticketService.purchaseTickets(VALID_ACCOUNT_ID, new TicketTypeRequest(Type.ADULT, 2));

            verify(seatReservationService, times(1)).reserveSeat(anyLong(), anyInt());
        }

        @Test
        @DisplayName("Takes payment before reserving seats")
        void shouldPayBeforeReservingSeats() {

            ticketService.purchaseTickets(VALID_ACCOUNT_ID, new TicketTypeRequest(Type.ADULT, 2));

            InOrder order = inOrder(ticketPaymentService, seatReservationService);

            order.verify(ticketPaymentService, times(1)).makePayment(anyLong(), anyInt());
            order.verify(seatReservationService, times(1)).reserveSeat(anyLong(), anyInt());
        }
    }
}