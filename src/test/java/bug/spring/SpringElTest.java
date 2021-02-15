package bug.spring;

import org.junit.jupiter.api.Test;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


public class SpringElTest {

	private CarSearch evalRule(Object object, String rule) {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext(object);
		return (CarSearch) parser.parseExpression(rule).getValue(context);
	}

	@Test
	public void evalObjectWithInternalStateWithSpringEl() {
		String redFilter = "#root.filterCars('red')";
		CarSearch carDB = getTestCars();
		CarSearch result = evalRule(carDB, redFilter);
		assertThat(result.dataList.size()).isEqualTo(2);
	}

	@Test
	public void evalObjectWithInternalStateWithSpringElAssignToMap() {
		String redFilter = "matches['red'] = #root.state.filterCars('red')";
		ExpressionParser parser = new SpelExpressionParser();
		ShowWork showWork = new ShowWork();
		showWork.setState(getTestCars());

		StandardEvaluationContext context = new StandardEvaluationContext(showWork);

		parser.parseExpression(redFilter).getValue(context);
		Map<String, Object> matches = showWork.getMatches();
		assertThat(matches.size()).isEqualTo(1);

		CarSearch redSearch = (CarSearch) matches.get("red");
		assertThat(redSearch.dataList.size()).isEqualTo(2);
	}

	@Test
	public void manuallyTestFilterCars() {
		CarSearch carDB = getTestCars();

		{
			CarSearch result = carDB.filterCars("red");
			assertThat(result.dataList.size()).isEqualTo(2);
		}

		{
			CarSearch result = carDB.filterCars("green");
			assertThat(result.dataList.size()).isEqualTo(1);
			Car car = result.dataList.get(0);
			assertThat(car.features).contains("green", "fwd");
		}


		{
			CarSearch result = carDB.filterCars("awd");
			assertThat(result.dataList.size()).isEqualTo(1);
			Car car = result.dataList.get(0);
			assertThat(car.features).contains("red", "awd");
		}
	}

	private CarSearch getTestCars() {
		Car redAwd = new Car.Builder()
				.feature("awd")
				.feature("red").build();
		Car redFwd = new Car.Builder()
				.feature("fwd")
				.feature("red").build();
		Car greenFwd = new Car.Builder()
				.feature("fwd")
				.feature("green").build();
		CarSearch carDB = new CarSearch(Arrays.asList(redAwd, redFwd, greenFwd));
		assertThat(carDB.dataList.size()).isEqualTo(3);
		return carDB;
	}

	public static class CarSearch {
		private List<Car> dataList;

		public CarSearch(List<Car> dataList) {
			this.dataList = dataList;
		}

		public CarSearch() {
			this(new ArrayList<>());
		}

		public CarSearch filterCars(String featureName) {
			List<Car> matching = dataList.stream()
					.filter(car -> car.features.contains(featureName))
					.collect(Collectors.toList());
			return new CarSearch(matching);
		}
	}

	public static class Car {
		private final List<String> features;

		public Car(List<String> features) {
			this.features = features;
		}

		public static class Builder {
			private final List<String> featureList = new ArrayList<>();

			public Car build() {
				return new Car(featureList);
			}

			public Builder feature(String feature) {
				featureList.add(feature);
				return this;
			}
		}
	}

	private static class ShowWork {
		CarSearch state;
		private final Map<String, Object> matches = new HashMap<>();

		public CarSearch getState() {
			return state;
		}

		public void setState(CarSearch state) {
			this.state = state;
		}

		public Map<String, Object> getMatches() {
			return matches;
		}
	}


}
