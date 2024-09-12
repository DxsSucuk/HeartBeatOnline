import type { UUID } from 'crypto';

const BASE_PATH = 'http://localhost:8080/';

export interface Gambles {
	user: Gambler;
	gambleID: UUID;
	gambleAmount: number;
	heartBeat: number;
	timestamp: string;
}

export interface Bet {
	amount: number;
	heartBeat: number;
}

export interface Gambler {
	id: UUID;
	name: string;
	money: number;
	wins: number;
	authToken: string;
}

export interface HeartBeat {
	beat: number;
	timestamp: string;
	source: string;
}

export function parseGambler(value: any) {
	const instance: Gambler = {
		id: value.id,
		money: value.money,
		name: value.name,
		wins: value.wins,
		authToken: value.authToken
	};

	return instance;
}

export function parseGambles(values: any) {
	let array = new Array<Gambles>();

	for (const value of values) {
		const instance: Gambles = {
			gambleAmount: value.gambleAmount,
			gambleID: value.gambleID,
			heartBeat: value.heartBeat,
			user: parseGambler(value.user),
			timestamp: value.timestamp
		};

		array.push(instance);
	}

	return array;
}

export function parseHeartBeat(value: any) {
	const instance: HeartBeat = {
		beat: value.beat,
		source: value.source,
		timestamp: value.timestamp
	};

	return instance;
}

export function parseHeartBeats(values: any) {
	let array = new Array<HeartBeat>();

	for (const value of values) {
		array.push(parseHeartBeat(value));
	}

	return array;
}

export async function getGambler(id: string | null) {
	const value = await get('gambler?id=' + id);
	if (value.success) {
		let gambler: Gambler = parseGambler(value.data);
		return gambler;
	}

	return null;
}

export async function getSelfGambler() {
	return getGambler(localStorage.getItem('id'));
}

export async function createGambler() {
	const value = await get('gambler/create');
	if (value.success) {
		let gambler: Gambler = parseGambler(value.data);
		localStorage.setItem('id', gambler.id);
		localStorage.setItem('auth', gambler.authToken);
		return gambler;
	}

	return null;
}

export async function getLeaderboard(typ: string) {
	const value = await get('leaderboard?typ=' + typ);
	if (value.success) {
		return parseHeartBeats(value.data);
	}

	return [];
}

export async function getHeartBeat() {
	const value = await get('heartbeat');
	if (value.success) {
		return parseHeartBeat(value.data);
	}

	return null;
}

export async function getGambles() {
	const value = await get('gambleboard');
	if (value.success) {
		return parseGambles(value.data);
	}

	return [];
}

export async function getNextPull() {
	const value = await get('next');
	if (value.success) {
		return value.data;
	}

	return '2024-08-30T22:02:57+00:00';
}

export async function putBet(beat: number, amount: number) {
	const value = await get(
		'gamble?id=' +
			localStorage.getItem('id') +
			'&beat=' +
			beat +
			'&amount=' +
			amount +
			'&token=' +
			localStorage.getItem('auth')
	);
	console.log(value.message);
	return value.success;
}

export async function post(path: string, body: string) {
	try {
		const res = await fetch(BASE_PATH + path, {
			method: 'POST',
			body: body,
			headers: {
				'Content-Type': 'application/json'
			}
		});

		if (res.status != 200) {
			console.error(res.status + ' ' + path);
			return { success: false };
		}

		const json = await res.json();
		if (!json.success) {
			console.error(json.message);
			return { success: false, message: json.message };
		}

		return json;
	} catch (err) {
		return { success: false, message: err };
	}
}

export async function get(path: string) {
	try {
		const res = await fetch(BASE_PATH + path, {
			method: 'GET',
			headers: {
				'ngrok-skip-browser-warning': 'tits'
			}
		});

		if (res.status != 200) {
			console.error(res.status + ' ' + path);
			return { success: false };
		}

		const json = await res.json();
		if (!json.success) {
			console.error(json.message);
			return { success: false, message: json.message };
		}

		return json;
	} catch (err) {
		return { success: false, message: err };
	}
}
