package cyh.ast.vo;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cyh.ast.annotation.DynamicField;
import cyh.ast.processor.DynamicFieldProcessor.DynamicValueProcessor;

@DynamicField
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

	// public DynamicVO(String s, int i, DynamicValue[] values) {
	// 	super(s, i);
	// 	process(values);
	// }
}
