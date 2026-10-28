# language: python, file: verify_model.py
from pathlib import Path
import sys

try:
    import tensorflow as tf
except ImportError:
    print("Missing dependency. Install with: python -m pip install tensorflow")
    sys.exit(2)

model_path = Path("app/src/main/assets/model.tflite")
expected_input = [1, 640, 640, 3]
expected_output = [1, 7, 8400]

if not model_path.is_file():
    print(f"Model not found: {model_path}")
    sys.exit(1)

try:
    interpreter = tf.lite.Interpreter(model_path=str(model_path))
    interpreter.allocate_tensors()
    inputs = interpreter.get_input_details()
    outputs = interpreter.get_output_details()
    if len(inputs) != 1 or len(outputs) != 1:
        raise ValueError(f"Expected one input/output, got {len(inputs)} input(s), {len(outputs)} output(s)")

    i, o = inputs[0], outputs[0]
    in_shape, out_shape = i["shape"].tolist(), o["shape"].tolist()
    print(f"Input: shape={in_shape}, dtype={i['dtype']}")
    print(f"Output: shape={out_shape}, dtype={o['dtype']}")

    errors = []
    if in_shape != expected_input:
        errors.append(f"Input shape expected {expected_input}, got {in_shape}")
    if out_shape != expected_output:
        errors.append(f"Output shape expected {expected_output}, got {out_shape}")
    if i["dtype"] != tf.float32:
        errors.append(f"Input dtype expected float32, got {i['dtype']}")
    if o["dtype"] != tf.float32:
        errors.append(f"Output dtype expected float32, got {o['dtype']}")
    if errors:
        print("\n".join(f"ERROR: {e}" for e in errors))
        sys.exit(1)
    print("Tensor shape/type checks passed; output semantics still need validation.")
except Exception as exc:
    print(f"Model validation failed: {exc}")
    sys.exit(1)
