<!-- YOU CAN DELETE EVERYTHING IN THIS PAGE -->
<script lang="ts">
	import Heart from '$lib/heart.svelte';
	import Leaderboard from '$lib/leaderboard.svelte';
	import Time from 'svelte-time/Time.svelte';
	import {
		Accordion,
		AccordionItem,
		Avatar,
		getToastStore,
		type ModalComponent,
		type ModalSettings,
		type ToastSettings
	} from '@skeletonlabs/skeleton';
	import {
		getGambles,
		getHeartBeat,
		getLeaderboard,
		getNextPull,
		getSelfGambler,
		putBet,
		type Bet,
		type Gambles,
		type HeartBeat
	} from '$lib/client';
	import gamblerWriteable from '$lib/gamblerWriteable';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import Gamblemodal from '$lib/gamblemodal.svelte';
	let leaderboardAllTime: HeartBeat[] = [];
	let leaderboardOfToday: HeartBeat[] = [];
	let lastBeat: HeartBeat;
	let nextPull: string;
	let bets: Gambles[] = [];

	onMount(async () => {
		await updateInfo();
	});

	const toastStore = getToastStore();
	const modalStore = getModalStore();

	const modalComponent: ModalComponent = { ref: Gamblemodal };
	const modal: ModalSettings = {
		type: 'component',
		component: modalComponent,
		response: (r: any) => {
			sendBet(r);
		}
	};

	async function sendBet(r: Bet) {
		let result: boolean = await putBet(r.heartBeat, r.amount);
		if (result) {
			toastStore.trigger(success);
			bets = await getGambles();
			let newGambler = await getSelfGambler();
			if (newGambler != undefined) {
				$gamblerWriteable = newGambler;
			}
		} else {
			toastStore.trigger(failed);
		}
	}

	const failed: ToastSettings = {
		message: 'Broke bitch most likely dunno.',
		// Provide any utility or variant background style:
		background: 'variant-filled-error',
		autohide: true,
		timeout: 5000
	};

	const success: ToastSettings = {
		message: 'Nice bet and good luck!',
		// Provide any utility or variant background style:
		background: 'variant-filled-success',
		autohide: true,
		timeout: 5000
	};

	const updated: ToastSettings = {
		message: 'Fetched the latest data!',
		// Provide any utility or variant background style:
		background: 'variant-filled-secondary',
		autohide: true,
		timeout: 5000
	};

	function runUpdater(ms: number) {
		if (ms < 1000) ms = Date.now() + 300000;
		setTimeout(() => {
			updateInfo();
		}, ms);
	}

	async function updateInfo() {
		bets = await getGambles();
		nextPull = await getNextPull();
		leaderboardAllTime = await getLeaderboard('all');
		leaderboardOfToday = await getLeaderboard('day');
		let beat = await getHeartBeat();
		if (beat !== undefined) {
			lastBeat = beat as HeartBeat;
		}
		toastStore.trigger(updated);
		runUpdater(Date.parse(nextPull) - Date.now());
	}
</script>

<div class="container h-full mx-auto flex justify-center items-center">
	<div style="position: absolute; right: 20px;">
		<Leaderboard
			title="Leaderboard All"
			entries={leaderboardAllTime}
			format="DD.MM.YYYY @ hh:mm"
			relative={false}
		/>
	</div>
	<div class="space-y-10 text-center flex flex-col items-center">
		<Heart />
		<div id="bpmCounter" class="text-primary-500">
			<p class="text-5xl">{lastBeat !== undefined ? lastBeat.beat : 'None'}</p>
		</div>
		<div id="lastUpdate">
			<span> Last update: </span>
			<Time
				relative
				timestamp={lastBeat !== undefined ? lastBeat.timestamp : '2024-08-30T22:02:57+00:00'}
			/>
		</div>
		<div>
			<button
				type="button"
				class="btn variant-filled-secondary"
				on:click={() => modalStore.trigger(modal)}
				disabled={$gamblerWriteable == undefined}
			>
				<span>Gamble!</span>
			</button>
		</div>
	</div>
	<div style="position: absolute; left: 20px;">
		<Leaderboard
			title="Leaderboard Today"
			entries={leaderboardOfToday}
			format="hh:mm"
			relative={false}
		/>
	</div>
	<div class="footer">
		<span> Next Data pull: </span>
		<Time live={60} timestamp={nextPull} relative />
	</div>
	<div class="footer-info">
		<Accordion class="card p-4 text-token backdrop-blur-lg">
			<AccordionItem>
				<svelte:fragment slot="lead"
					><i class="fa-solid fa-dice text-xl w-6 text-center"></i></svelte:fragment
				>
				<svelte:fragment slot="summary"><p class="font-bold">Gambling? What?</p></svelte:fragment>
				<svelte:fragment slot="content">
					<p>
						To make this website more fun and interesting, you can bet on the highest BPM value<br
						/>
						for a day! And if you are correct you either get the full amount or it will be split<br
						/>
						between everyone that bet correctly!
					</p>
				</svelte:fragment>
			</AccordionItem>
			<AccordionItem>
				<svelte:fragment slot="lead"
					><i class="fa-solid fa-wallet text-xl w-6 text-center"></i></svelte:fragment
				>
				<svelte:fragment slot="summary"><p class="font-bold">Do I have to pay?</p></svelte:fragment>
				<svelte:fragment slot="content">
					<p>
						No! You will be "gambling" none existing money. So no worries, you will never actually <br
						/>
						loose your money!
					</p>
				</svelte:fragment>
			</AccordionItem>
		</Accordion>
	</div>
	<div
		class="gambling text-center flex flex-col items-center self-center card text-token backdrop-blur-lg"
	>
		<strong class="text-xl uppercase p-4">Top Gambles of Today</strong>
		<ul role="list" class="p-4">
			{#if bets !== undefined}
				{#each bets as entry, index}
					<li class="flex justify-between gap-x-6 py-5">
						<div class="flex min-w-0 gap-x-4">
							<Avatar initials={index + 1 + '.'} class="h-12 w-12 flex-none rounded-full" />
						</div>
						<div class="flex min-w-0 gap-x-4">
							<div class="min-w-0 flex-auto sm:flex sm:flex-col sm:items-start sm:self-center">
								<p class="font-semibold leading-6">{entry.user.name}</p>
							</div>
						</div>
						<div class="hidden shrink-0 sm:flex sm:flex-col sm:items-end sm:self-center">
							<p class="text-sm leading-6">
								<span class="text-primary-500">{entry.gambleAmount}</span>$ on
								<span class="text-primary-500">{entry.heartBeat}</span> BPM
							</p>
						</div>
					</li>
				{/each}
			{/if}
		</ul>
	</div>
</div>

<style lang="css">
	#bpmCounter {
		font-size: 24px;
		margin-top: 20px;
	}

	#lastUpdate {
		font-size: 12px;
		margin-top: 5px;
	}

	.footer {
		position: absolute;
		bottom: 20px;
		font-size: 12px;
	}

	.footer-info {
		position: absolute;
		bottom: 20px;
		left: 20px;
	}

	.gambling {
		position: absolute;
		bottom: 20px;
		right: 20px;
	}
</style>
