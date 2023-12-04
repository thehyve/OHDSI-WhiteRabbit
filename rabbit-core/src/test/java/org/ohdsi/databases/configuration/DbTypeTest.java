package org.ohdsi.databases.configuration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DbTypeTest {

    @Test
    void testPickList() {
        List<String> labelsFromAllDbTypeValues = Stream.of(DbType.values()).map(DbType::label).sorted().collect(Collectors.toList());
        List<String> labelsFromPickList = Stream.of(DbType.pickList()).sorted().collect(Collectors.toList());

        assertEquals(labelsFromAllDbTypeValues, labelsFromPickList, "The picklist should contain all the labels defined in the DbType enum");
    }
}