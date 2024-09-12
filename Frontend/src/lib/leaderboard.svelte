<script lang="ts">
	import Time from 'svelte-time';
	import type { HeartBeat } from './client';

	export var title: string;
	export var entries: HeartBeat[];
	export var format: string;
	export var relative: boolean;
</script>

<div class="leaderboard card p-4">
	<h3>{title}</h3>
	<ul>
		{#if entries !== undefined}
			{#each entries as entry, index}
				<li>
					<span>
						{index + 1}. {entry.beat} BPM at
					</span>
					<Time
						{relative}
						timestamp={entry.timestamp !== undefined ? entry.timestamp : '2024-09-09'}
						{format}
					/>
				</li>
			{/each}
		{/if}
	</ul>
</div>

<style>
	.leaderboard {
		top: 50%;
		transform: translateY(-50%);
		width: 250px;
	}

	.leaderboard h3 {
		text-align: center;
		margin: 0;
		margin-bottom: 10px;
		font-size: 18px;
	}

	.leaderboard ul {
		list-style: none;
		padding: 0;
		margin: 0;
	}

	.leaderboard li {
		font-size: 16px;
		padding: 5px 0;
		border-bottom: 1px solid #ddd;
	}

	.leaderboard li:last-child {
		border-bottom: none;
	}
</style>
