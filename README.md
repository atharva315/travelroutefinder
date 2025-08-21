# TravelRouteFinder Telegram Bot

**Author:** Atharva Gaikwad

**Copyright:** 2025 Atharva. All Rights Reserved.

## Overview

TravelRouteFinder is a Telegram bot that optimizes travel routes for real-world cities or locations using real-time road distances with the OpenRouteService API. Users interact with the bot directly in Telegram, entering cities or famous places one by one (e.g. Mumbai, Pune, Nagpur), and the bot replies with the truly optimal round trip (TSP) route and the total distance, computed using actual road networks.

## Features

- Accepts any city or landmark name as input via Telegram chat
- Fetches real-world geolocations and driving distances using OpenRouteService
- Finds the best short roundtrip (Travelling Salesman Problem) using road (not air) distances
- Outputs the complete optimized route (with names) and total kilometers
- Built in Java with TelegramBots, OkHttp, and GSON
- Maintained and copyright by Atharva

## How to Use

1. Start the bot on Telegram with `/start`
2. Enter locations one by one (each on a new message)
3. Type `done` when finished
4. The bot replies with the optimal route and real total kilometers

## Example

/start
Mumbai
Pune
Nagpur
Chennai
done

Optimal route: Mumbai → Pune → Nagpur → Chennai → Mumbai
Total KM: 2350 km


## Uniqueness

- The first Java Telegram bot to combine Telegram chat, OpenRouteService API, and real-time TSP optimization for road distances.
- Designed for travelers, logistics, tourism, and anyone wanting perfect road trip planning.
- Algorithm and conversation flow written entirely by Atharva.

## License and Protection

This project is protected under a custom copyright license (see LICENSE file).  
All source code, logic, and UX flow are copyright © 2025 Atharva. All rights reserved.

---

**For special use, collaborations, or business/academic licensing, contact Atharva.**
