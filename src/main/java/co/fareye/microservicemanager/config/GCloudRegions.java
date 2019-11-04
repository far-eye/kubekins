package co.fareye.microservicemanager.config;

public enum GCloudRegions {
    REGION1A("Asia-East 1 A", "asia-east1-a"),
    REGION1B("Asia-East 1 B", "asia-east1-b"),
    REGION1C("Asia-East 1 C", "asia-east1-c"),
    REGION2A("Asia-East 2 A", "asia-east2-a"),
    REGION2B("Asia-East 2 B", "asia-east2-b"),
    REGION2C("Asia-East 2 C", "asia-east2-c"),
    REGION3A("Asia-NorthEast 1 A", "asia-northeast1-a"),
    REGION3B("Asia-NorthEast 1 B", "asia-northeast1-b"),
    REGION3C("Asia-NorthEast 1 C", "asia-northeast1-c"),
    REGION4A("Asia-South 1 A", "asia-south1-a"),
    REGION4B("Asia-South 1 B", "asia-south1-b"),
    REGION4C("Asia-South 1 C", "asia-south1-c"),
    REGION5A("Asia-SouthEast 1 A", "asia-southeast1-a"),
    REGION5B("Asia-SouthEast 1 B", "asia-southeast1-b"),
    REGION5C("Asia-SouthEast 1 C", "asia-southeast1-c"),
    REGION6A("Australia-SouthEast 1 A", "australia-southeast1-a"),
    REGION6B("Australia-SouthEast 1 B", "australia-southeast1-b"),
    REGION6C("Australia-SouthEast 1 C", "australia-southeast1-c"),
    REGION7A("Europe-North 1 A", "europe-north1-a"),
    REGION7B("Europe-North 1 B", "europe-north1-b"),
    REGION7C("Europe-North 1 C", "europe-north1-c"),
    REGION8B("Europe-West 1 B", "europe-west1-b"),
    REGION8C("Europe-West 1 C", "europe-west1-c"),
    REGION8D("Europe-West 1 D", "europe-west1-d"),
    REGION9A("Europe-West 2 A", "europe-west2-a"),
    REGION9B("Europe-West 2 B", "europe-west2-b"),
    REGION9C("Europe-West 2 C", "europe-west2-c"),
    REGION10A("Europe-West 3 A", "europe-west3-a"),
    REGION10B("Europe-West 3 B", "europe-west3-b"),
    REGION10C("Europe-West 3 C", "europe-west3-c"),
    REGION11A("Europe-West 4 A", "europe-west4-a"),
    REGION11B("Europe-West 4 B", "europe-west4-b"),
    REGION11C("Europe-West 4 C", "europe-west4-c"),
    REGION12A("NorthAmerica-NorthEast 1 A", "northamerica-northeast1-a"),
    REGION12B("NorthAmerica-NorthEast 1 B", "northamerica-northeast1-b"),
    REGION12C("NorthAmerica-NorthEast 1 C", "northamerica-northeast1-c"),
    REGION13A("SouthAmerica-East 1 A", "southamerica-east1-a"),
    REGION13B("SouthAmerica-East 1 B", "southamerica-east1-b"),
    REGION13C("SouthAmerica-East 1 C", "southamerica-east1-c"),
    REGION14A("US-Central 1 A", "us-central1-a"),
    REGION14B("US-Central 1 B", "us-central1-b"),
    REGION14C("US-Central 1 C", "us-central1-c"),
    REGION14F("US-Central 1 F", "us-central1-f"),
    REGION15B("US-East 1 B", "us-east1-b"),
    REGION15C("US-East 1 C", "us-east1-c"),
    REGION15D("US-East 1 D", "us-east1-d"),
    REGION16A("US-East 4 A", "us-east4-a"),
    REGION16B("US-East 4 B", "us-east4-b"),
    REGION16C("US-East 4 C", "us-east4-c"),
    REGION17A("US-West 1 A", "us-west1-a"),
    REGION17B("US-West 1 B", "us-west1-b"),
    REGION17C("US-West 1 C", "us-west1-c"),
    REGION18A("US-West 2 A", "us-west2-a"),
    REGION18B("US-West 2 B", "us-west2-b"),
    REGION18C("US-West 2 C", "us-west2-c");

    private final String name;
    private final String code;

    GCloudRegions(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }
}