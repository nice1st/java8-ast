package cyh.ast.vo;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cyh.ast.annotation.DynamicArgsConstructor;
import cyh.ast.processor.DynamicArgsConstructorProcessor.DynamicValueProcessor;

@DynamicArgsConstructor
public class DynamicVO extends StaticVO implements DynamicValueProcessor<DynamicValue> {

	private List<String> dynamicFieldValues;

	@Override
	public void process(DynamicValue[] values) {
		this.dynamicFieldValues = Stream.of(values)
		  .map(Optional::ofNullable)
		  .map(dynamicValue -> {
			  if (dynamicValue.isPresent()) {
				  return dynamicValue.get().getValue();
			  } else {
				  return "";
			  }
		  })
		  .collect(Collectors.toList());
	}
}
