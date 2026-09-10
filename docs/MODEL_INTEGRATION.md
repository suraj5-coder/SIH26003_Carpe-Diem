# On-device model integration

MindMate Base must remain small and fully usable without model packs. Load one model family only while its feature is open, close native sessions when the screen leaves, and never keep multiple interpreters resident.

## Face detection and embeddings

Integration boundary: `ai/face/FaceEmbeddingEngine.kt`.

Recommended pipeline:

1. Use MediaPipe Face Detector to locate and align a single face.
2. Reject no-face, multiple-face, or very small detections.
3. Normalize the aligned crop exactly as required by the selected embedding model.
4. Run a licensed INT8 MobileFaceNet or lightweight ArcFace model through LiteRT.
5. L2-normalize the embedding.
6. Encrypt its compact byte representation using `security/LocalVault`.
7. Calibrate similarity thresholds on representative Indian faces and target devices.
8. Display recognition as a game answer, never as high-security identity proof.

Do not bundle an unverified model or return synthetic embeddings. Add liveness only if there is a validated, low-cost implementation; MobileFaceNet alone is vulnerable to photo spoofing.

## Offline speech packs

Integration boundary: `ai/speech/OfflineSpeechEngine.kt`.

Package English, Hindi, and Assamese independently. A pack should contain only:

- one quantized AI4Bharat IndicConformer-compatible acoustic model,
- tokenizer/vocabulary assets,
- a lightweight VAD such as quantized Silero VAD,
- language metadata and checksums.

Prefer ONNX Runtime Mobile with a reduced operator configuration when using ONNX. Use LiteRT when a converted model has been validated for equivalent output and lower device cost. Packs must be explicitly installed before offline use; there is no cloud fallback.

For every pack:

- stream 16 kHz mono PCM,
- gate inference with VAD,
- bound utterance duration,
- expose memory/thermal failure states,
- release sessions after the feature closes,
- retain large audio only with caretaker consent.

## Song comparison

Integration boundary: `ai/music/MusicSimilarity.kt`.

Required implementation before enabling a score:

1. Decode reference and microphone audio to mono PCM.
2. Detect voiced frames using VAD and/or energy thresholds.
3. Extract pitch with YIN or pYIN and chroma features using bounded windows.
4. Normalize key/register differences where musically appropriate.
5. Compare expected and sung sequences with the provided DTW utility.
6. Optionally compare lyrics only when a real offline speech pack is available.
7. Combine calibrated components into a configurable 70–80% game threshold.

The result must be labeled “game performance,” never a medical or dementia score.

## OCR

`ai/ocr/PrescriptionOcr.kt` uses bundled ML Kit recognizers and therefore works on first launch without model download. Latin and Devanagari models are included. Assamese prescription OCR requires a validated Bengali-script recognizer integration; the app currently leaves uncertain text for manual editing rather than claiming support.

OCR extraction is deliberately conservative. Never create a medicine or alarm directly from OCR output.

## Resource budget

Before accepting any model pack, test on a low-end ARM64 device and document:

- compressed and installed size,
- peak proportional set size (PSS),
- cold start time,
- inference latency,
- battery and thermal behavior,
- model license and source,
- representative accuracy and known limitations.

Release native resources in `close()` and test repeated screen entry for leaks.
