package test.aiTests;

import org.javatuples.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * utility class for testing
 * @param <T>
 */
public class testMethods<T extends Comparable<T>> {
//    /**
//     *
//     * @param l1 first list
//     * @param l2 second list
//     * @return if both lists have the same values according to Objects.equals()
//     */
//        public boolean hasSameValues(List<T> l1, List<T> l2){
//            if (l1.size() != l2.size()) return false;
//            for (int i = 0; i < l1.size(); i++){
//                if (!Objects.equals(l1.get(i), l2.get(i))) return false;
//            }
//            return true;
//        }
//        public boolean isSamePair(Pair<List<T>, T> pair1, Pair<List<T>, T> pair2){
//            return hasSameValues(pair1.getValue0(), pair2.getValue0()) && pair1.getValue1().equals(pair2.getValue1());
//        }
//        public Pair<List<T>, T> createPairList(T secondValue, T ...listValues){
//            return new Pair<>(Arrays.stream(listValues).toList(), secondValue);
//        }
}
