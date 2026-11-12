<?php
declare(strict_types=1);

$translation = '';
$error = '';
$text = $_POST['text'] ?? '';
$sourceLanguage = $_POST['sourceLanguage'] ?? 'unspecified';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (trim($text) === '') {
        $error = 'Text is required.';
    } else {
        $backendUrl = rtrim((string) getenv('TRANSLATOR_BACKEND_URL'), '/');
        $username = (string) getenv('TRANSLATOR_USERNAME');
        $password = (string) getenv('TRANSLATOR_PASSWORD');

        if ($backendUrl === '' || $username === '' || $password === '') {
            $error = 'Configure TRANSLATOR_BACKEND_URL, TRANSLATOR_USERNAME, and TRANSLATOR_PASSWORD.';
        } else {
            $payload = json_encode(
                ['text' => $text, 'sourceLanguage' => $sourceLanguage],
                JSON_UNESCAPED_UNICODE | JSON_THROW_ON_ERROR
            );
            $context = stream_context_create(['http' => [
                'method' => 'POST',
                'header' => implode("\r\n", [
                    'Authorization: Basic ' . base64_encode($username . ':' . $password),
                    'Content-Type: application/json; charset=utf-8',
                    'Accept: application/json',
                ]),
                'content' => $payload,
                'timeout' => 30,
                'ignore_errors' => true,
            ]]);

            $body = @file_get_contents($backendUrl . '/api/translate', false, $context);
            $statusLine = $http_response_header[0] ?? '';
            preg_match('/\s(\d{3})\s/', $statusLine, $matches);
            $status = isset($matches[1]) ? (int) $matches[1] : 0;
            $data = is_string($body) ? json_decode($body, true) : null;

            if ($status >= 200 && $status < 300 && is_string($data['translation'] ?? null)) {
                $translation = trim($data['translation']);
                if ($translation === '') {
                    $error = "Backend returned an empty 'translation' field.";
                }
            } elseif (is_array($data) && is_string($data['message'] ?? null)) {
                $error = 'Backend error' . ($status ? " (HTTP $status)" : '') . ': ' . $data['message'];
            } else {
                $error = $status ? "Backend returned HTTP $status." : 'Could not reach the backend.';
            }
        }
    }
}

function html(string $value): string {
    return htmlspecialchars($value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
}
?>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Darija Translator</title>
    <style>
        body { font-family: system-ui, sans-serif; max-width: 48rem; margin: 3rem auto; padding: 0 1rem; background: #f7f5ef; color: #1d2925; }
        textarea, input { width: 100%; box-sizing: border-box; padding: .75rem; margin: .35rem 0 1rem; border: 1px solid #8aa096; border-radius: .5rem; }
        button { background: #176b4d; color: white; border: 0; border-radius: .5rem; padding: .75rem 1.25rem; font-weight: 700; }
        .result, .error { margin-top: 1.5rem; padding: 1rem; border-radius: .5rem; }
        .result { background: white; border-inline-start: .3rem solid #176b4d; font-size: 1.25rem; }
        .error { background: #ffe8e6; color: #8b1e18; }
    </style>
</head>
<body>
<h1>Darija Translator</h1>
<form method="post">
    <label for="sourceLanguage">Source language (optional)</label>
    <input id="sourceLanguage" name="sourceLanguage" value="<?= html($sourceLanguage) ?>">
    <label for="text">Text to translate</label>
    <textarea id="text" name="text" rows="8" required><?= html($text) ?></textarea>
    <button type="submit">Translate to Darija</button>
</form>
<?php if ($translation !== ''): ?>
    <section class="result" aria-live="polite" dir="rtl"><?= nl2br(html($translation)) ?></section>
<?php endif; ?>
<?php if ($error !== ''): ?>
    <section class="error" role="alert"><?= html($error) ?></section>
<?php endif; ?>
</body>
</html>
