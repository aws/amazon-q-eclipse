// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import software.amazon.awssdk.regions.Region;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AwsRegionTest {

    private AwsRegion result;

    @Test
    public void testUsWestRegion() {
        result = AwsRegion.from(Region.US_WEST_1);
        assertEquals("us-west-1", result.id());
        assertEquals("US West (N. California)", result.name());
        assertEquals("aws", result.partitionId());
        assertEquals("North America", result.category());
        assertEquals("(N. California) (us-west-1)", result.displayName());

        String toStringExpected = "{\"id\":\"us-west-1\","
            + "\"name\":\"US West (N. California)\","
            + "\"partitionId\":\"aws\","
            + "\"category\":\"North America\","
            + "\"displayName\":\"(N. California) (us-west-1)\"}";

        assertEquals(toStringExpected, result.toString());
    }

    @Test
    public void testUsEastRegion() {
        result = AwsRegion.from(Region.US_EAST_1);
        assertEquals("us-east-1", result.id());
        assertEquals("US East (N. Virginia)", result.name());
        assertEquals("aws", result.partitionId());
        assertEquals("North America", result.category());
        assertEquals("(N. Virginia) (us-east-1)", result.displayName());

        String toStringExpected = "{\"id\":\"us-east-1\","
            + "\"name\":\"US East (N. Virginia)\","
            + "\"partitionId\":\"aws\","
            + "\"category\":\"North America\","
            + "\"displayName\":\"(N. Virginia) (us-east-1)\"}";

        assertEquals(toStringExpected, result.toString());
    }

    @Test
    public void testEuNorthRegion() {
        result = AwsRegion.from(Region.EU_NORTH_1);
        assertEquals("eu-north-1", result.id());
        assertEquals("Europe (Stockholm)", result.name());
        assertEquals("aws", result.partitionId());
        assertEquals("Europe", result.category());
        assertEquals("(Stockholm) (eu-north-1)", result.displayName());

        String toStringExpected = "{\"id\":\"eu-north-1\","
            + "\"name\":\"Europe (Stockholm)\","
            + "\"partitionId\":\"aws\","
            + "\"category\":\"Europe\","
            + "\"displayName\":\"(Stockholm) (eu-north-1)\"}";

        assertEquals(toStringExpected, result.toString());
    }

    @Test
    public void testAfSouthRegion() {
        result = AwsRegion.from(Region.AF_SOUTH_1);
        assertEquals("af-south-1", result.id());
        assertEquals("Africa (Cape Town)", result.name());
        assertEquals("aws", result.partitionId());
        assertEquals("Africa", result.category());
        assertEquals("Africa (af-south-1)", result.displayName());

        String toStringExpected = "{\"id\":\"af-south-1\","
            + "\"name\":\"Africa (Cape Town)\","
            + "\"partitionId\":\"aws\","
            + "\"category\":\"Africa\","
            + "\"displayName\":\"Africa (af-south-1)\"}";

        assertEquals(toStringExpected, result.toString());
    }

    @Test
    public void testCaCentralRegion() {
        result = AwsRegion.from(Region.CA_CENTRAL_1);
        assertEquals("ca-central-1", result.id());
        assertEquals("Canada (Central)", result.name());
        assertEquals("aws", result.partitionId());
        assertEquals("North America", result.category());
        assertEquals("Canada (Central) (ca-central-1)", result.displayName());

        String toStringExpected = "{\"id\":\"ca-central-1\","
            + "\"name\":\"Canada (Central)\","
            + "\"partitionId\":\"aws\","
            + "\"category\":\"North America\","
            + "\"displayName\":\"Canada (Central) (ca-central-1)\"}";

        assertEquals(toStringExpected, result.toString());
    }

    @Test
    public void testApNortheastRegion() {
        result = AwsRegion.from(Region.AP_NORTHEAST_2);
        assertEquals("ap-northeast-2", result.id());
        assertEquals("Asia Pacific (Seoul)", result.name());
        assertEquals("aws", result.partitionId());
        assertEquals("Asia Pacific", result.category());
        assertEquals("Asia Pacific (ap-northeast-2)", result.displayName());

        String toStringExpected = "{\"id\":\"ap-northeast-2\","
            + "\"name\":\"Asia Pacific (Seoul)\","
            + "\"partitionId\":\"aws\","
            + "\"category\":\"Asia Pacific\","
            + "\"displayName\":\"Asia Pacific (ap-northeast-2)\"}";

        assertEquals(toStringExpected, result.toString());
    }

    @Test
    public void testSaEastRegion() {
        result = AwsRegion.from(Region.SA_EAST_1);
        assertEquals("sa-east-1", result.id());
        assertEquals("South America (Sao Paulo)", result.name());
        assertEquals("aws", result.partitionId());
        assertEquals("South America", result.category());
        assertEquals("South America (sa-east-1)", result.displayName());

        String toStringExpected = "{\"id\":\"sa-east-1\","
            + "\"name\":\"South America (Sao Paulo)\","
            + "\"partitionId\":\"aws\","
            + "\"category\":\"South America\","
            + "\"displayName\":\"South America (sa-east-1)\"}";

        assertEquals(toStringExpected, result.toString());
    }

    @Test
    public void testCnNorthRegion() {
        result = AwsRegion.from(Region.CN_NORTH_1);
        assertEquals("cn-north-1", result.id());
        assertEquals("China (Beijing)", result.name());
        assertEquals("aws-cn", result.partitionId());
        assertEquals("China", result.category());
        assertEquals("China (cn-north-1)", result.displayName());

        String toStringExpected = "{\"id\":\"cn-north-1\","
            + "\"name\":\"China (Beijing)\","
            + "\"partitionId\":\"aws-cn\","
            + "\"category\":\"China\","
            + "\"displayName\":\"China (cn-north-1)\"}";

        assertEquals(toStringExpected, result.toString());
    }

    @Test
    public void testMeSouthRegion() {
        result = AwsRegion.from(Region.ME_SOUTH_1);
        assertEquals("me-south-1", result.id());
        assertEquals("Middle East (Bahrain)", result.name());
        assertEquals("aws", result.partitionId());
        assertEquals("Middle East", result.category());
        assertEquals("Middle East (me-south-1)", result.displayName());

        String toStringExpected = "{\"id\":\"me-south-1\","
            + "\"name\":\"Middle East (Bahrain)\","
            + "\"partitionId\":\"aws\","
            + "\"category\":\"Middle East\","
            + "\"displayName\":\"Middle East (me-south-1)\"}";

        assertEquals(toStringExpected, result.toString());
    }

    @Test
    public void testIlCentralRegion() {
        result = AwsRegion.from(Region.IL_CENTRAL_1);
        assertEquals("il-central-1", result.id());
        assertEquals("Israel (Tel Aviv)", result.name());
        assertEquals("aws", result.partitionId());
        assertNull(result.category());
        assertEquals("Israel (Tel Aviv)", result.displayName());

        String toStringExpected = "{\"id\":\"il-central-1\","
            + "\"name\":\"Israel (Tel Aviv)\","
            + "\"partitionId\":\"aws\","
            + "\"category\":\"null\","
            + "\"displayName\":\"Israel (Tel Aviv)\"}";

        assertEquals(toStringExpected, result.toString());
    }
}
