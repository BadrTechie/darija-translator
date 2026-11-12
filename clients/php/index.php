<?php
declare(strict_types=1);

header('Cache-Control: no-store');
header('Referrer-Policy: no-referrer');

$translation = '';
$error = '';
$text = (string) ($_POST['text'] ?? '');
$sourceLanguage = (string) ($_POST['sourceLanguage'] ?? 'unspecified');
$username = (string) ($_POST['username'] ?? '');
$password = (string) ($_POST['password'] ?? '');
$backendUrl = rtrim(trim((string) (getenv('TRANSLATOR_BACKEND_URL') ?: 'http://localhost:9080')), '/');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (trim($text) === '') {
        $error = 'Text is required.';
    } elseif (trim($username) === '' || $password === '') {
        $error = 'Backend username and password are required.';
    } elseif (!isHttpUrl($backendUrl)) {
        $error = 'TRANSLATOR_BACKEND_URL must be a valid HTTP or HTTPS URL.';
    } elseif (!filter_var(ini_get('allow_url_fopen'), FILTER_VALIDATE_BOOLEAN)) {
        $error = 'This PHP installation must enable allow_url_fopen.';
    } else {
        try {
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

            // The password stays in this server-side request header. It is never
            // added to a URL, logged by this client, or rendered back into HTML.
            $http_response_header = [];
            $body = @file_get_contents($backendUrl . '/api/translate', false, $context);
            $status = responseStatus($http_response_header);
            $data = decodeResponse($body);

            if ($status >= 200 && $status < 300) {
                $candidate = $data['translation'] ?? null;
                if (!is_string($candidate) || trim($candidate) === '') {
                    $error = "The backend returned no usable 'translation' field.";
                } else {
                    $translation = trim($candidate);
                }
            } else {
                $error = backendError($status, $data);
            }
        } catch (JsonException $exception) {
            $error = 'Could not encode the request as UTF-8 JSON.';
        }
    }
}

// Do not retain the submitted password while rendering the response.
$password = '';

/** @param string[] $responseHeaders */
function responseStatus(array $responseHeaders): int
{
    $statusLine = $responseHeaders[0] ?? '';
    return preg_match('/\s(\d{3})(?:\s|$)/', $statusLine, $matches) === 1
        ? (int) $matches[1]
        : 0;
}

/** @return array<string, mixed> */
function decodeResponse(string|false $body): array
{
    if (!is_string($body) || trim($body) === '') {
        return [];
    }

    try {
        $data = json_decode($body, true, 512, JSON_THROW_ON_ERROR);
        return is_array($data) ? $data : [];
    } catch (JsonException $exception) {
        return [];
    }
}

/** @param array<string, mixed> $data */
function backendError(int $status, array $data): string
{
    $message = is_string($data['message'] ?? null) ? trim($data['message']) : '';

    return match ($status) {
        0 => 'Could not reach the translation backend. Check that Liberty is running and the backend URL is correct.',
        400 => $message !== '' ? 'Invalid request: ' . $message : 'The backend rejected the submitted text.',
        401, 403 => 'The backend rejected those credentials. Check the username and password.',
        502, 503, 504 => $message !== ''
            ? 'Translation provider error: ' . $message
            : "The translation provider is unavailable (HTTP $status). Try again.",
        default => $message !== ''
            ? "Backend error (HTTP $status): $message"
            : "The backend returned HTTP $status.",
    };
}

function isHttpUrl(string $url): bool
{
    if (filter_var($url, FILTER_VALIDATE_URL) === false) {
        return false;
    }

    $scheme = strtolower((string) parse_url($url, PHP_URL_SCHEME));
    return $scheme === 'http' || $scheme === 'https';
}

function html(string $value): string
{
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
        .hint { margin-top: -.6rem; color: #4f625a; font-size: .9rem; }
        .result, .error { margin-top: 1.5rem; padding: 1rem; border-radius: .5rem; }
        .result { background: white; border-inline-start: .3rem solid #176b4d; font-size: 1.25rem; }
        .error { background: #ffe8e6; color: #8b1e18; }
    </style>
</head>
<body>
<h1>Darija Translator</h1>
<form method="post" action="" accept-charset="UTF-8">
    <label for="username">Backend username</label>
    <input id="username" name="username" autocomplete="username" required value="<?= html($username) ?>">
    <label for="password">Backend password</label>
    <input id="password" name="password" type="password" autocomplete="current-password" required>
    <p class="hint">The password is sent only in this POST and is never redisplayed.</p>
    <label for="sourceLanguage">Source language (optional)</label>
    <input id="sourceLanguage" name="sourceLanguage" value="<?= html($sourceLanguage) ?>">
    <label for="text">Text to translate</label>
    <textarea id="text" name="text" rows="8" required><?= html($text) ?></textarea>
    <button type="submit">Translate to Darija</button>
</form>
<?php if ($translation !== ''): ?>
    <section class="result" aria-live="polite" lang="ar" dir="rtl"><?= nl2br(html($translation), false) ?></section>
<?php endif; ?>
<?php if ($error !== ''): ?>
    <section class="error" role="alert"><?= html($error) ?></section>
<?php endif; ?>
</body>
</html>
