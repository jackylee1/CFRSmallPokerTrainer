package cfr.trainer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cfr.trainer.action.Action;
import cfr.trainer.games.Game;
import cfr.trainer.games.GameDescription;
import cfr.trainer.games.GameFactory;
import cfr.trainer.node.InfoSetFactory;
import cfr.trainer.node.Node;
import cfr.trainer.node.NodeImpl;

public class CFRPlusTrainer {

	public Map<String, double[]> regretMap = new HashMap<String, double[]>();
	double util = 0;
	int utilCount;
	double averageGameValue = 0;

	public static void main(String[] args) throws Exception {
		int iterations = 1;
		new CFRPlusTrainer().train(GameDescription.ROYAL_RHODE_ISLAND_HEADSUP_NO_LIMIT_POKER, iterations);
	}

	public void train(GameDescription gameDescription, int iterations) throws Exception {

		Game gameStructure = GameFactory.setUpGame(gameDescription, 2);
		List<List<Integer>> validChanceCombinations = gameStructure.getListOfValidChanceCombinations();
		for (int i = 0; i < iterations; i++) {
			int combo = 0;
			for (List<Integer> validCombo : validChanceCombinations) {
				combo++;
				// System.out.println("Combination :" + combo);
				Game game = GameFactory.setUpGame(gameDescription, 2);
				game.startGame();
				game.setValidChanceCombinations(validCombo);
				util += cfrPlus(game, 1, 1,0, i);
				util += cfrPlus(game, 1, 1,1, i);
			}
		}
		averageGameValue = util / (iterations * validChanceCombinations.size() * 2);
		System.out.println("Average game value: " +averageGameValue+ "\n Nodes: " + regretMap.size());
		writeStrategyMapToJSONFile(regretMap);
	}

	private double cfrPlus(Game game, double p0, double p1,int playerToTrain, int iteration) throws Exception {
//		System.out.println("training player:" + playerToTrain);
		if (game.isAtTerminalNode()) {
			return game.getPayOffs().get(game.getPlayerToAct());
		}
		if (game.isAtChanceNode()) {
			game.performChanceAction();
		}

		// Get Node
		String nodeId = game.getNodeIdWithActionMemory();
		// String nodeId = game.getNodeIdWithGameState();

		double[] regretSums = regretMap.get(nodeId);
		if (regretSums == null) {
			regretSums = new double[game.constructActionArray().length];
			regretMap.put(nodeId, regretSums);
		}
		// recursively call cfr
		int player = game.getPlayerToAct();

		double[] strategy = calculateStrategy(regretSums);
		int actionsAvailable = regretSums.length;
		double[] util = new double[actionsAvailable];
		double nodeUtil = 0;

		Action[] actionArray = game.constructActionArray();
		for (int action = 0; action < actionsAvailable; action++) {
			Game copyOfGame = GameFactory.copyGame(game);
			copyOfGame.performAction(player, actionArray[action]);

			util[action] = player == 0 ? -cfrPlus(copyOfGame, p0 * strategy[action], p1,playerToTrain, iteration)
					: -cfrPlus(copyOfGame, p0, p1 * strategy[action],playerToTrain, iteration);

			nodeUtil += strategy[action] * util[action];
		}

		// compute cfr
		if (playerToTrain == player) {
			for (int a = 0; a < actionsAvailable; a++) {
				double regret = util[a] - nodeUtil;
//				double regretSum = regretSums[a] + (player == 0 ? p1 : p0) * regret;
//				node.regretSum[a] = Math.max(regretSum, 0);
				double weightedRegret = regret* iteration;
				double regretSum = (regretSums[a] + (player == 0 ? p1 : p0) * weightedRegret);
				regretSums[a] = Math.max(regretSum, 0);
			}
		}
		return nodeUtil;
	}

	public double getAverageGameValue() {
		return averageGameValue;
	}

	public Map<String, double[]> getRegretMap() {
		return regretMap;
	}

	private void writeStrategyMapToJSONFile(Map nodeMap)
			throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.writerWithDefaultPrettyPrinter()
				.writeValue(new File("C:\\Users\\James\\Desktop\\StrategyMaps\\user.json"), nodeMap);
	}

	private double[] calculateStrategy(double[] regretSum) {
		int numOfActions = regretSum.length;
		double[] strategy = new double[numOfActions];
		double normalizingSum = 0;
		for (int a = 0; a < numOfActions; a++) {
			// only taking positive regrets
			strategy[a] = regretSum[a] > 0 ? regretSum[a] : 0;
			normalizingSum += strategy[a];
		}
		for (int a = 0; a < numOfActions; a++) {
			if (normalizingSum > 0)
				strategy[a] /= normalizingSum;
			else
				strategy[a] = 1.0 / numOfActions;
		}
		return strategy;
	}

	public Map<String, double[]> getStrategyMap() {
		Map<String, double[]> strategyMap = new HashMap<>();
		for (Entry<String, double[]> entry : regretMap.entrySet()) {
			strategyMap.put(entry.getKey(), calculateStrategy(entry.getValue()));
		}
		return strategyMap;
	}
}
