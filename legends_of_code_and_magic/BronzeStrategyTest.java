import java.util.*;
import java.io.*;
import java.math.*;
import java.util.stream.Collectors;

class TestableGameState extends GameState {
    public void setPlayerHand(Hand cards) {
        this.playerHand = cards;
    }
}

public class BronzeStrategyTest {
    public static void main(String[] args) {
        BronzeStrategyTest test = new BronzeStrategyTest();
        test.testChooseSummons();
    }
    public void testChooseSummons() {
        // mana, attack - 3,4 - 4,3 - 2,2 - 5,2 - 8,6 - 3,1 - 2,1 for 5 available mana should return 1, 3
        TestableGameState testGameState = new TestableGameState();
        Hand testHand = new Hand();
        testHand.addCard(new Card(0, 0, 0, 0, 3, 4, 0, "", 0, 0, 0));
        testHand.addCard(new Card(0, 1, 0, 0, 4, 3, 0, "", 0, 0, 0));
        testHand.addCard(new Card(0, 2, 0, 0, 2, 2, 0, "", 0, 0, 0));
        testHand.addCard(new Card(0, 3, 0, 0, 5, 2, 0, "", 0, 0, 0));
        testHand.addCard(new Card(0, 4, 0, 0, 8, 6, 0, "", 0, 0, 0));
        testHand.addCard(new Card(0, 5, 0, 0, 3, 1, 0, "", 0, 0, 0));
        testHand.addCard(new Card(0, 6, 0, 0, 2, 1, 0, "", 0, 0, 0));
        testGameState.setPlayerHand(testHand);
        testGameState.setPlayerMana(5);
        BronzeStrategy strategy = new BronzeStrategy();

        ArrayList<SummonAction> summons = strategy.chooseSummons(testGameState);
        String output = summons.stream()
            .map(Action::toString)
            .collect(Collectors.joining(";"));

        int numActions = 2;
        String firstAction = "SUMMON 0";
        String secondAction = "SUMMON 2";

        assert summons.size() == numActions : "Length should be " + numActions + ". Output: " + output;
        assert summons.get(0).toString().equals(firstAction) : "First action should be " + firstAction + ". Output: " + output;
        assert summons.get(1).toString().equals(secondAction) : "Second action should be " + secondAction + ". Output: " + output;
    }

}