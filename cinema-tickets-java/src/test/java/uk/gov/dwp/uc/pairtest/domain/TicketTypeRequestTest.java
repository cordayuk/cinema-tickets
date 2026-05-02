package uk.gov.dwp.uc.pairtest.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;


class TicketTypeRequestTest {

    @Test
    @DisplayName("Creation of TicketTypeRequest should throw IllegalArgumentException if Type is null")
    void shouldThrowIllegalArgumentExceptionsWhenTypeIsNull() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TicketTypeRequest(null, 10));

        assertEquals("Type cannot be null", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -2, -6, -14, -25, -33, -44 })
    @DisplayName("Creation of TicketTypeRequest should throw IllegalArgumentException if noOfTickets must be greater or equal than zero")
    void shouldThrowIllegalArgumentExceptionsWhenNoOfTicketsIsBelowZero(int noOfTickets) {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TicketTypeRequest(Type.ADULT, noOfTickets));

        assertEquals("noOfTickets cannot be less than 0", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 6, 8, 14, 16, 25, 89})
    @DisplayName("Creation of TicketTypeRequest should be successful if noOfTickets is equal or greater than zero")
    void shouldCreateTicketTypeRequestIfNoOfTicketsIsGreaterThan1(int noOfTickets) {

        TicketTypeRequest ticketTypeRequest = new TicketTypeRequest(Type.ADULT, noOfTickets);

        assertEquals(noOfTickets, ticketTypeRequest.noOfTickets());
    }

    @ParameterizedTest
    @EnumSource(Type.class)
    @DisplayName("Creation of TicketTypeRequest should be successful when provided type")
    void shouldCreateTicketTypeRequestWhenGivenType(Type type) {
        TicketTypeRequest ticketTypeRequest = new TicketTypeRequest(type, 1);

        assertEquals(type, ticketTypeRequest.type());
    }

    @Test
    @DisplayName("Adult tickets should cost 25")
    void adultTypeTicketsShouldHaveCostOf25() {

        assertEquals(25, Type.ADULT.getCost());
    }

    @Test
    @DisplayName("Child tickets should cost 15")
    void childTypeTicketsShouldHaveCostOf15() {

        assertEquals(15, Type.CHILD.getCost());
    }

    @Test
    @DisplayName("Infant tickets should cost zero")
    void infantTypeTicketsShouldHaveCostOfZero() {

        assertEquals(0, Type.INFANT.getCost());
    }

    @Test
    @DisplayName("Adult Tickets should require a seat")
    void adultTicketsShouldRequireASeat() {

        assertTrue(Type.ADULT.isSeatRequired());
    }

    @Test
    @DisplayName("Child Tickets should require a seat")
    void childTicketsShouldRequireASeat() {

        assertTrue(Type.CHILD.isSeatRequired());
    }

    @Test
    @DisplayName("Infant Tickets should not require a seat")
    void childTicketsShouldNotRequireASeat() {

        assertFalse(Type.INFANT.isSeatRequired());
    }
}