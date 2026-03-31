<?php
$files = array_values(array_filter(scandir(__DIR__), function ($file) {
    return is_file(__DIR__ . DIRECTORY_SEPARATOR . $file)
        && preg_match('/\.html?$/i', $file);
}));

natcasesort($files);
$files = array_values($files);
?>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>File HTML</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 40px;
            background: #f5f5f5;
            color: #222;
        }

        h1 {
            margin-bottom: 24px;
        }

        .list {
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
        }

        a {
            text-decoration: none;
        }

        button {
            padding: 12px 18px;
            border: 0;
            border-radius: 8px;
            background: #0b5ed7;
            color: #fff;
            cursor: pointer;
            font-size: 14px;
        }

        button:hover {
            background: #094db1;
        }

        .empty {
            padding: 16px;
            background: #fff;
            border-radius: 8px;
        }
    </style>
</head>
<body>
    <h1>AI TOOLS</h1>

    <?php if (empty($files)): ?>
        <div class="empty">Nessun file HTML trovato nella cartella corrente.</div>
    <?php else: ?>
        <div class="list">
            <?php foreach ($files as $file): ?>
                <a href="<?= htmlspecialchars($file, ENT_QUOTES, 'UTF-8') ?>">
                    <button type="button"><?= htmlspecialchars($file, ENT_QUOTES, 'UTF-8') ?></button>
                </a>
            <?php endforeach; ?>
        </div>
    <?php endif; ?>
</body>
</html>
