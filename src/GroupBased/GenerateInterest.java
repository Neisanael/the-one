package GroupBased;

import GroupBased.Model.EventData;
import GroupBased.Model.FilterData;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class GenerateInterest {

    public static Set<FilterData> generateFilterData() {
        Set<FilterData> filters = new HashSet<>();
        Random rand = new Random();

        while (filters.size() < 5) {
            boolean topicValue = rand.nextBoolean();
            int min = rand.nextInt(30); // Generate a random min value between 0 and 29

            // Ensure max is at least min + 1, but not exceeding 30
            int max;
            if (min == 29) {
                // Special case: if min is 29, max can only be 30
                max = 30;
            } else {
                max = rand.nextInt(30 - min) + min + 1; // min+1 to ensure max > min
            }

            filters.add(new FilterData(topicValue, min, max));
        }

        return filters;
    }

    public static Set<EventData> generateEventData() {
        Set<EventData> events = new HashSet<>();
        Random rand = new Random();

        while (events.size() < 5) {
            boolean topicValue = rand.nextBoolean();
            int min = rand.nextInt(30); // Generate a random min value between 0 and 30
            events.add(new EventData(topicValue, min));
        }

        return events;
    }

    public static void main(String[] args) {
        System.out.println(GenerateInterest.generateEventData());
        System.out.println(GenerateInterest.generateFilterData());
    }

}
