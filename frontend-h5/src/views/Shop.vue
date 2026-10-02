<template>
  <div class="page">
    <TabBar current="shop" />

    <header class="shop-head card">
      <div>
        <div class="head-title">🎁 积分商城</div>
        <div class="head-sub">攒够积分，兑换喜欢的奖品吧</div>
      </div>
      <div class="points-box">
        <span class="points-num">{{ data?.points ?? store.points }}</span>
        <span class="points-label">我的积分</span>
      </div>
    </header>

    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      奖品加载中…
    </div>

    <template v-else-if="data">
      <div v-if="data.prizes.length === 0" class="empty card">
        <span class="emoji">🎁</span>
        奖品还没有上架，等家长添加吧
      </div>

      <div v-else class="grid">
        <div v-for="p in data.prizes" key="p.id" class="prize card">
          <div class="prize-img">
            <img v-if="p.image" :src="resolveImg(p.image)" alt="" />
            <span v-else class="img-fallback">🎁</span>
            <span v-if="p.stock <= 0" class="soldout">已兑完</span>
          </div>
          <div class="prize-name">{{ p.name }}</div>
          <div v-if="p.description" class="prize-desc">{{ p.description }}</div>
          <div class="prize-foot">
            <span class="cost">{{ p.pointsCost }} 分</span>
            <span class="stock">剩 {{ p.stock }} 份</span>
          </div>
          <button
            class="btn redeem-btn"
            :class="p.canRedeem ? 'btn-primary' : 'btn-disabled'"
            :disabled="!p.canRedeem"
            @click="openRedeem(p)"
          >
            {{ p.stock <= 0 ? '已兑完' : p.enoughPoints ? '我要兑换' : `还差 ${p.pointsCost - (data.points)} 分` }}
          </button>
        </div>
      </div>
    </template>

    <!-- 兑换确认 -->
    <div v-if="target" class="mask" @click.self="target = null">
      <div class="modal">
        <h2 class="modal-title">确认兑换？</h2>
        <div class="confirm-prize">
          <div class="cp-img">
            <img v-if="target.image" :src="resolveImg(target.image)" alt="" />
            <span v-else>🎁</span>
          </div>
          <div class="cp-name">{{ target.name }}</div>
          <div class="cp-cost">需要 {{ target.pointsCost }} 积分</div>
          <div class="cp-balance">兑换后余额：{{ (data?.points ?? 0) - target.pointsCost }} 分</div>
        </div>
        <div class="modal-actions">
          <button class="btn btn-ghost" @click="target = null">再想想</button>
          <button class="btn btn-primary" :disabled="redeeming" @click="doRedeem">
            {{ redeeming ? '提交中…' : '确认兑换' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 兑换成功 -->
    <div v-if="success" class="mask">
      <div class="modal">
        <div class="success-ico">🎉</div>
        <h2 class="modal-title">申请已提交！</h2>
        <p class="success-text">
          「{{ success.prizeName }}」兑换申请已发给家长，<br />
          审核通过后就会发给你啦～
        </p>
        <div class="success-points">剩余积分：{{ success.points }}</div>
        <button class="btn btn-primary btn-block" @click="finishSuccess">好的</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import TabBar from '@/components/TabBar.vue'
import { childApi } from '@/api/child'
import { useChildStore } from '@/stores/child'
import { showToast } from '@/composables/useToast'
import type { PrizeItem, PrizeList, RedeemResult } from '@/types/api'

const store = useChildStore()
const loading = ref(true)
const data = ref<PrizeList | null>(null)
const target = ref<PrizeItem | null>(null)
const redeeming = ref(false)
const success = ref<RedeemResult | null>(null)

function resolveImg(image: string): string {
  if (/^https?:\/\//.test(image)) return image
  return image.startsWith('/') ? image : `/${image}`
}

async function load() {
  loading.value = true
  try {
    data.value = await childApi.prizeList()
  } catch (e) {
    showToast((e as Error).message, 'error')
  } finally {
    loading.value = false
  }
}

function openRedeem(p: PrizeItem) {
  if (!p.canRedeem) return
  target.value = p
}

async function doRedeem() {
  if (!target.value || redeeming.value) return
  redeeming.value = true
  try {
    const res = await childApi.redeem(target.value.id)
    success.value = res
    target.value = null
    await load()
  } catch (e) {
    showToast((e as Error).message, 'error')
  } finally {
    redeeming.value = false
  }
}

function finishSuccess() {
  success.value = null
}

onMounted(load)
</script>

<style scoped>
.shop-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
}

.head-title {
  font-size: 23px;
  font-weight: 800;
}

.head-sub {
  color: var(--text-sub);
  font-size: 14px;
  margin-top: 2px;
}

.points-box {
  text-align: center;
  background: var(--warning-soft);
  border-radius: 14px;
  padding: 8px 16px;
}

.points-num {
  display: block;
  font-size: 24px;
  font-weight: 800;
  color: #ffb347;
  line-height: 1.2;
}

.points-label {
  font-size: 13px;
  color: #ffb347;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-top: 4px;
}

.prize {
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.prize-img {
  position: relative;
  height: 130px;
  border-radius: 14px;
  background: var(--bg);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  margin-bottom: 12px;
}

.prize-img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.img-fallback {
  font-size: 56px;
}

.soldout {
  position: absolute;
  inset: 0;
  background: rgba(40, 46, 70, 0.55);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
}

.prize-name {
  font-size: 18px;
  font-weight: 700;
}

.prize-desc {
  color: var(--text-sub);
  font-size: 14px;
  margin: 2px 0 8px;
  line-height: 1.5;
  flex: 1;
}

.prize-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.cost {
  font-size: 19px;
  font-weight: 800;
  color: #ffb347;
}

.stock {
  font-size: 13px;
  color: var(--text-sub);
}

.redeem-btn {
  width: 100%;
}

.btn-disabled {
  background: #3c484f;
  color: #8fa0aa;
  cursor: not-allowed;
  box-shadow: none;
}

.confirm-prize {
  text-align: center;
  margin-bottom: 14px;
}

.cp-img {
  width: 110px;
  height: 110px;
  border-radius: 16px;
  background: var(--bg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;
  overflow: hidden;
}

.cp-img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.cp-img span {
  font-size: 50px;
}

.cp-name {
  font-size: 20px;
  font-weight: 700;
}

.cp-cost {
  color: #ffb347;
  font-weight: 700;
  margin-top: 4px;
}

.cp-balance {
  color: var(--text-sub);
  font-size: 15px;
  margin-top: 2px;
}

.modal-actions {
  display: flex;
  gap: 12px;
}

.modal-actions .btn {
  flex: 1;
}

.success-ico {
  font-size: 60px;
  text-align: center;
}

.success-text {
  text-align: center;
  font-size: 17px;
  line-height: 1.7;
  margin-bottom: 10px;
}

.success-points {
  text-align: center;
  color: var(--text-sub);
  margin-bottom: 18px;
}
</style>
