# 🎯 LinkedIn Games Tracker

[![Telegram Bot](https://img.shields.io/badge/telegram-@LinkedInGamesTrackerBot-blue?logo=telegram)](https://t.me/LinkedInGamesTrackerBot)
[![Build Status](https://drone.nasvigo.com/api/badges/rubasace/linkedin-games-tracker/status.svg)](https://drone.nasvigo.com/rubasace/linkedin-games-tracker)
[![License: MIT](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

A Telegram bot designed to streamline and gamify the experience of solving LinkedIn's daily puzzles  
(currently, [Queens](https://www.linkedin.com/games/queens), [Tango](https://www.linkedin.com/games/tango), and [Zip](https://www.linkedin.com/games/zip)).

Each Telegram group acts as its own competition, where everyday members submit their puzzle results via screenshots. The bot processes them using OCR to maintain scoreboards and
rankings.

## 🤖 Try It Out

👉 Add the bot to your Telegram group: [@LinkedInGamesTrackerBot](https://t.me/LinkedInGamesTrackerBot)

Each group is treated as a standalone competition — just invite the bot to your group and you’re ready to go!

## ⚙️ How It Works

When you add the bot to a Telegram group, that group becomes its own independent leaderboard and competition space. Each day, members of the group can submit their results for
LinkedIn’s puzzles (currently: Queens, Tango, and Zip) by simply uploading a screenshot of their completion screen.

The bot automatically scans these screenshots using OCR (powered by OpenCV and Tesseract) and extracts the relevant information: the game type and the time it took to solve it.
Once
processed, your time is recorded for the current day and associated to your Telegram user id.

As group members submit their scores, the bot keeps track of who’s already participated and waits for everyone to submit. Once all registered players have sent their times, the bot
automatically recalculates and publishes the daily leaderboard. Alternatively, any member can run the /daily command to manually trigger a recalculation at any time.

If not everyone submits, the bot will still calculate and publish the results at the end of the day, including only the times that were received. This ensures the competition
continues smoothly even if someone forgets to post their score.

Each group maintains its own isolated set of scores, players, and competition history — meaning users can participate in multiple groups independently. Leaderboards reset daily, so
every new day is a fresh challenge for members to compete, improve, and (hopefully) brag.

> [!NOTE]
> Only **group messages** with commands or screenshots are processed.  
> Private message support is under development.

## 🛠️ Commands

| Command                               | Description                                                                                                                                |
|---------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| `/join`                               | Explicitly registers you to compete in the group (optional, done automatically the moment you message in the group).                       |
| `/games`                              | Lists the games being tracked by the bot.                                                                                                  |
| `/delete <game>`                      | Removes your score for today's selected game.                                                                                              |
| `/deleteAll`                          | Removes all of your submitted results for the current day.                                                                                 |
| `/override @<username> <game> <time>` | Admin only: Override today's user time (`mm:ss`) for a given game.                                                                         |
| `/daily`                              | Calculates and displays the current leaderboard for the group. It will be recalculated automatically after all members submit their times. |
| `/help`                               | Displays a list of available commands and usage instructions.                                                                              |

## 🚀 Getting Started

### Requirements

- A Telegram account
- A Telegram group where you have permission to add bots

### Setup

1. **Invite the bot to your group**  
   The bot will start tracking scores and players from the moment it's added.

2. **Run `/join` (optional)**  
   Registers you in the group manually. Members are also automatically registered the first time they submit a valid screenshot or are added to the group after tracking begins.

3. **Submit your LinkedIn puzzle screenshot**  
   Send a screenshot of your completed puzzle. The bot will process and register your time for the current day.

4. (Optional) **Manage your score**  
   Use `/delete`, `/deleteall`, or `/override` (admin-only) to modify the results if needed. Useful for correcting misprocessed or mistaken submissions.

5. (Optional) **Track the competition**  
   Use `/daily` to recalculate and view the group leaderboard for the day.
6. **Wait for the group**  
   When all members are done (or at the end of the day) they leaderboard gets recalculated and shown in the group.

## 💻 Tech Stack

- **Language**: Java 21
- **OCR Engine**: [Tesseract](https://github.com/tesseract-ocr/tesseract) via [Tess4J](https://github.com/nguyenq/tess4j)
- **Image Processing**: [OpenCV](https://github.com/opencv/opencv) via [Bytedeco](https://github.com/bytedeco)
- **Frameworks**: [Spring Boot](https://github.com/spring-projects/spring-boot) + [TelegramBots](https://github.com/rubenlagus/TelegramBots)

## 🔮 Future Features

- [ ] Automatic reminders for users missing their submissions near the end of the day
- [ ] Auto-finalize scores at end-of-day, even if some users didn’t submit
- [ ] Allow to opt-in/out from games on each group
- [ ] Support for private chat submissions (auto-publish to all groups the user is in)
- [ ] Web dashboard showing historical and aggregated performance across groups and users

## 🤝 Contributing

We’d love your help to improve this project!

Whether you're here to fix a bug, suggest a feature, or simply explore how it works, here’s how to get involved:

1. ⭐ **Star this repository** to show support
2. 🐞 **Report issues** or request features via [GitHub Issues](https://github.com/rubasace/linkedin-games-tracker/issues)
3. 🛠️ **Submit a pull request** with enhancements, fixes, or new ideas
4. 📣 **Spread the word** — share it with your Telegram puzzle groups!

## 📄 License

This project is licensed under the [MIT License](LICENSE).